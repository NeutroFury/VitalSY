package com.jonesys.vitalsy.service;

import com.jonesys.vitalsy.dto.response.TimelineEventDto;
import com.jonesys.vitalsy.model.Usuario;
import com.jonesys.vitalsy.repository.TimelineRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio orquestador de la línea de tiempo cronológica unificada.
 *
 * RESPONSABILIDAD Única:
 * Transforma el Object[] plano que devuelve la consulta UNION ALL nativa
 * en una lista tipada de TimelineEventDto lista para ser serializada al LLM.
 *
 * ESTRATEGIA DE VENTANA TEMPORAL:
 * En lugar de restar días simples (now - windowDays), se calcula el inicio
 * de la ventana como el comienzo del día local del usuario hace `windowDays` días.
 *
 * ESTRATEGIA DE MUESTREO (TOKEN BUDGET):
 * Los sensores CGM (FreeStyle Libre vía LibreLinkUp) generan ~288 lecturas/día
 * (1 cada 5 minutos). En una ventana de 7 días eso son ~2.016 eventos GLUCOSE.
 * Enviar todos al LLM es ineficiente y puede exceder el context window.
 * Solución: submuestrear GLUCOSE a 1 lectura por hora cuando se supera el umbral.
 * Los eventos manuales (INSULIN, MEAL) NUNCA se submuestrean.
 */
@Service
@Transactional(readOnly = true)
public class TimelineAssemblerService {

    private static final Logger log = LoggerFactory.getLogger(TimelineAssemblerService.class);

    /**
     * Máximo de eventos GLUCOSE antes de activar el submuestreo.
     * Por debajo de este umbral se envían todas las lecturas (análisis de mayor resolución).
     * Por encima, se submuestrea a 1 lectura/hora.
     *
     * Racional del valor:
     *   - 1 lectura/hora × 24h × 7 días = 168 lecturas (manejable para el LLM)
     *   - 288 lecturas/día (sensor CGM) × 7 días = 2.016 (demasiado sin submuestrear)
     *   - Umbral 200: cubre ~1 semana a 1 lectura/30min antes de activar
     */
    private static final int MAX_GLUCOSE_EVENTS = 200;

    private final TimelineRepository timelineRepository;

    public TimelineAssemblerService(TimelineRepository timelineRepository) {
        this.timelineRepository = timelineRepository;
    }

    /**
     * Construye la línea de tiempo cronológica unificada para una ventana de días.
     *
     * @param usuario    El paciente cuyo historial se analiza. Se usa su ZoneId para
     *                   calcular los límites de la ventana en su hora local real.
     * @param windowDays Número de días hacia atrás a incluir (ej. 7, 14, 30).
     *                   El sistema incluye el día completo de inicio (desde 00:00:00).
     * @return Lista de eventos ordenada cronológicamente (ASC), lista para el LLM.
     *         Devuelve lista vacía (nunca null) si no hay datos en la ventana.
     */
    public List<TimelineEventDto> buildTimeline(Usuario usuario, int windowDays) {
        // ── 1. Calcular la ventana temporal en la zona local del paciente ──────────
        ZonedDateTime now         = ZonedDateTime.now(usuario.getZoneId());
        LocalDate     startDate   = now.toLocalDate().minusDays(windowDays);
        ZonedDateTime windowStart = startDate.atStartOfDay(usuario.getZoneId());
        ZonedDateTime windowEnd   = now;

        log.info("🕐 Construyendo timeline para usuario={} | ventana={} días | {} → {}",
                usuario.getId(), windowDays, windowStart, windowEnd);

        // ── 2. Ejecutar la consulta UNION ALL nativa ───────────────────────────────
        List<Object[]> rawRows;
        try {
            rawRows = timelineRepository.findUnifiedTimeline(
                    usuario.getId(), windowStart, windowEnd
            );
        } catch (Exception e) {
            log.error("❌ Error ejecutando la consulta UNION ALL de timeline para usuario={}: {}",
                    usuario.getId(), e.getMessage(), e);
            return Collections.emptyList();
        }

        log.info("✅ Timeline recuperada: {} eventos en {} días para usuario={}",
                rawRows.size(), windowDays, usuario.getId());

        // ── 3. Mapear Object[] → TimelineEventDto ─────────────────────────────────
        List<TimelineEventDto> allEvents = rawRows.stream()
                .map(this::mapRowToDto)
                .collect(Collectors.toList());

        // ── 4. Aplicar muestreo inteligente si hay demasiadas lecturas de glucosa ───
        return sampleTimeline(allEvents, usuario);
    }

    /**
     * Submuestrea la timeline cuando las lecturas de glucosa superan {@link #MAX_GLUCOSE_EVENTS}.
     *
     * ALGORITMO:
     *   - Separa los eventos en dos grupos: GLUCOSE y NO_GLUCOSE (insulina, comidas).
     *   - Los NO_GLUCOSE se preservan íntegramente (son escasos y clínicamente críticos).
     *   - Si GLUCOSE supera el umbral, se agrupa por hora UTC y se toma la primera lectura
     *     de cada hora (la más antigua del intervalo), garantizando distribución uniforme.
     *   - El resultado se fusiona y reordena cronológicamente (ASC).
     *
     * ELECCIÓN DE "PRIMERA DEL INTERVALO" vs "PROMEDIO":
     *   Se usa la primera lectura del intervalo en lugar de un promedio porque:
     *   a) El LLM necesita timestamps reales y específicos para el razonamiento causal.
     *   b) Un valor promedio no corresponde a ningún momento real, lo que puede
     *      confundir al LLM al correlacionar con eventos de insulina o comidas.
     *
     * @param events   Lista completa de eventos, ordenada ASC por timestamp
     * @param usuario  Paciente (usado solo para logging)
     * @return Lista submuestreada si es necesario, o la lista original sin cambios
     */
    private List<TimelineEventDto> sampleTimeline(List<TimelineEventDto> events, Usuario usuario) {
        // Separar por tipo
        List<TimelineEventDto> glucoseEvents = events.stream()
                .filter(e -> "GLUCOSE".equals(e.eventType()))
                .collect(Collectors.toList());
        List<TimelineEventDto> manualEvents = events.stream()
                .filter(e -> !"GLUCOSE".equals(e.eventType()))
                .collect(Collectors.toList());

        if (glucoseEvents.size() <= MAX_GLUCOSE_EVENTS) {
            // Sin submuestreo necesario
            log.info("📈 Timeline sin submuestreo: {} glucose + {} manuales = {} total",
                    glucoseEvents.size(), manualEvents.size(), events.size());
            return events;
        }

        // Submuestrear: agrupar por franja de 1 hora UTC y tomar la primera lectura de cada franja
        Map<String, TimelineEventDto> byHour = new LinkedHashMap<>();
        for (TimelineEventDto event : glucoseEvents) {
            if (event.timestamp() == null) continue;
            // Clave = "2026-06-08T14" (fecha + hora en UTC) → garantiza distribución uniforme
            String hourKey = event.timestamp()
                    .withZoneSameInstant(ZoneOffset.UTC)
                    .toLocalDateTime()
                    .toString()
                    .substring(0, 13); // "YYYY-MM-DDTHH"
            byHour.putIfAbsent(hourKey, event); // solo guarda la primera del intervalo
        }

        List<TimelineEventDto> sampledGlucose = new ArrayList<>(byHour.values());

        log.info("🔄 Submuestreo aplicado: {} lecturas GLUCOSE → {} (1/hora) + {} manuales = {} total",
                glucoseEvents.size(), sampledGlucose.size(), manualEvents.size(),
                sampledGlucose.size() + manualEvents.size());

        // Fusionar y reordenar cronológicamente
        List<TimelineEventDto> merged = new ArrayList<>(sampledGlucose);
        merged.addAll(manualEvents);
        merged.sort((a, b) -> {
            if (a.timestamp() == null) return -1;
            if (b.timestamp() == null) return  1;
            return a.timestamp().compareTo(b.timestamp());
        });
        return merged;
    }

    /**
     * Convierte una fila Object[] del ResultSet nativo en un TimelineEventDto tipado.
     *
     * ORDEN DE COLUMNAS (definido en TimelineRepository.findUnifiedTimeline):
     *   [0] timestamp       → java.sql.Timestamp (TIMESTAMPTZ desde PostgreSQL, en UTC)
     *   [1] event_type      → String
     *   [2] numeric_value   → BigDecimal o Double (nunca null en glucose/insulin/meal)
     *   [3] numeric_value2  → BigDecimal o Double o null
     *   [4] label           → String o null
     *   [5] subtype         → String o null
     *   [6] notes           → String o null
     *
     * MANEJO DE TIPOS JDBC:
     * El driver PostgreSQL retorna TIMESTAMPTZ como java.sql.Timestamp.
     * Lo convertimos a ZonedDateTime en UTC; la conversión a la zona del usuario
     * se aplica en la capa de presentación/LLM si se requiere mostrar hora local.
     *
     * @param row Array de 7 objetos con los valores de la fila del ResultSet
     * @return TimelineEventDto correctamente tipado
     */
    private TimelineEventDto mapRowToDto(Object[] row) {
        ZonedDateTime timestamp    = toZonedDateTime(row[0]);
        String        eventType    = (String)  row[1];
        Double        numericValue = toDouble(row[2]);
        Double        numericValue2= toDouble(row[3]);
        String        label        = (String)  row[4];
        String        subtype      = (String)  row[5];
        String        notes        = (String)  row[6];

        return new TimelineEventDto(
                timestamp,
                eventType,
                numericValue,
                numericValue2,
                label,
                subtype,
                notes
        );
    }

    // ── Helpers de conversión de tipos JDBC ───────────────────────────────────────

    /**
     * Convierte el valor JDBC de timestamp a ZonedDateTime.
     *
     * El driver PostgreSQL puede retornar:
     *   - java.sql.Timestamp (modo estándar JDBC, el más común con Hibernate)
     *   - java.time.OffsetDateTime (si el driver usa el modo preferido de Java 8)
     *
     * Ambos casos se manejan de forma segura. El resultado siempre queda en UTC
     * para mantener consistencia con nuestra configuración de sesión JDBC.
     */
    private ZonedDateTime toZonedDateTime(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Timestamp ts) {
            return ts.toInstant().atZone(ZoneOffset.UTC);
        }
        if (obj instanceof java.time.OffsetDateTime odt) {
            return odt.atZoneSameInstant(ZoneOffset.UTC);
        }
        if (obj instanceof ZonedDateTime zdt) {
            return zdt.withZoneSameInstant(ZoneOffset.UTC);
        }
        if (obj instanceof java.time.Instant inst) {
            return inst.atZone(ZoneOffset.UTC);
        }
        log.warn("⚠️ Tipo de timestamp inesperado en timeline: {}", obj.getClass().getName());
        return null;
    }

    /**
     * Convierte BigDecimal, Double, Integer o Float a Double de forma segura.
     * PostgreSQL puede retornar valores numéricos como BigDecimal dependiendo
     * de la precisión declarada en la columna (DECIMAL, NUMERIC).
     */
    private Double toDouble(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Double  d)   return d;
        if (obj instanceof BigDecimal b) return b.doubleValue();
        if (obj instanceof Integer  i)  return i.doubleValue();
        if (obj instanceof Long     l)  return l.doubleValue();
        if (obj instanceof Float    f)  return f.doubleValue();
        return null;
    }
}
