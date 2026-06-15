package com.jonesys.vitalsy.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jonesys.vitalsy.dto.response.TimelineEventDto;
import com.jonesys.vitalsy.model.GlucoseReading;
import com.jonesys.vitalsy.model.ParametroClinico;
import com.jonesys.vitalsy.model.Usuario;

import java.time.Duration;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Utilidad de construcción de prompts para el LLM (Gemini).
 *
 * Contiene dos métodos principales:
 *
 *   1. {@link #buildPrompt} — Método LEGADO. Construye un prompt de texto plano
 *      para el análisis descriptivo rápido de las últimas 3 lecturas de glucosa.
 *      Mantenido para no romper el flujo de {@code GeminiInferenceService}.
 *
 *   2. {@link #buildPredictivePayload} — Método NUEVO (predictivo). Construye un
 *      JSON estructurado que contiene el perfil del paciente, la línea de tiempo
 *      cronológica completa (glucosa + insulina + comidas) y las instrucciones
 *      exactas para que el LLM produzca un análisis causal sin alucinaciones.
 */
public class PromptBuilderUtil {

    // ── ObjectMapper local (sin Spring context) para serializar la timeline a JSON ──
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // ─────────────────────────────────────────────────────────────────────────────
    // MÉTODO LEGADO — Análisis descriptivo simple (flujo GeminiInferenceService)
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Construye el prompt de texto plano para el análisis causal rápido.
     * Solo utiliza las últimas 3 lecturas de glucosa. Mantenido sin cambios
     * para preservar compatibilidad con {@code GeminiInferenceService}.
     *
     * @param readings Últimas 3 lecturas de glucosa (de más reciente a más antigua)
     * @param usuario  Paciente cuyo perfil clínico se incluye
     * @return Prompt de texto listo para enviar a Gemini
     */
    public static String buildPrompt(List<GlucoseReading> readings, Usuario usuario) {
        StringBuilder sb = new StringBuilder();
        sb.append("Actúa como un Endocrinólogo y Experto en Diabetes de clase mundial.\n");
        sb.append("Analiza la causalidad de la tendencia de glucosa del paciente de forma anónima.\n\n");

        sb.append("DATOS CLÍNICOS DEL PACIENTE:\n");
        sb.append("- Coeficientes Médicos:\n");
        if (usuario != null) {
            double ratioIc = usuario.getRatioIc() != null ? usuario.getRatioIc() : 15.0;
            double factorIs = usuario.getFactorIs() != null ? usuario.getFactorIs() : 50.0;
            double peso = usuario.getPesoActual() != null ? usuario.getPesoActual() : 70.0;
            double altura = usuario.getAltura() != null ? usuario.getAltura() : 1.70;
            String insulinaLenta = usuario.getInsulinaLenta() != null ? usuario.getInsulinaLenta() : "Lantus (Predeterminada)";
            String insulinaRapida = usuario.getInsulinaRapida() != null ? usuario.getInsulinaRapida() : "Humalog (Predeterminada)";

            sb.append("  * Ratio IC (Relación Insulina-Carbohidratos): ").append(ratioIc).append(" g\n");
            sb.append("  * Factor IS (Factor de Sensibilidad a la Insulina): ").append(factorIs).append(" mg/dL por unidad\n");
            sb.append("  * Peso Actual: ").append(peso).append(" kg\n");
            sb.append("  * Altura: ").append(altura).append(" m\n");
            sb.append("  * Insulina Lenta (Basal): ").append(insulinaLenta).append("\n");
            sb.append("  * Insulina Rápida (Bolus): ").append(insulinaRapida).append("\n");
        } else {
            sb.append("  * Ratio IC: No configurado (usar estándar estimado)\n");
            sb.append("  * Factor IS: No configurado (usar estándar estimado)\n");
            sb.append("  * Peso Actual: No configurado (usar estándar estimado)\n");
            sb.append("  * Altura: No configurado (usar estándar estimado)\n");
            sb.append("  * Insulina Lenta: No configurada (usar estándar estimado)\n");
            sb.append("  * Insulina Rápida: No configurada (usar estándar estimado)\n");
        }

        sb.append("- Historial de las Últimas 3 Lecturas (de la más reciente a la más antigua):\n");
        ZonedDateTime ahora = ZonedDateTime.now();
        for (int i = 0; i < readings.size(); i++) {
            GlucoseReading r = readings.get(i);
            long minutosAtras = Duration.between(r.getFechaHora(), ahora).toMinutes();
            sb.append(String.format("  * Lectura %d: %d mg/dL (hace %d minutos), Tendencia: %s, Origen: %s\n",
                    i + 1, r.getValorMgdl(), minutosAtras, r.getTendencia() != null ? r.getTendencia() : "Estable", r.getTipoRegistro()));
        }

        sb.append("\nINSTRUCCIÓN DE ANÁLISIS:\n");
        sb.append("1. Analiza si la velocidad de cambio de glucemia presenta un riesgo inminente en los próximos 60 a 120 minutos ");
        sb.append("de cruzar los 60 mg/dL (hipoglucemia severa) o los 250 mg/dL (hiperglucemia severa).\n");
        if (usuario != null) {
            double peso = usuario.getPesoActual() != null ? usuario.getPesoActual() : 70.0;
            double altura = usuario.getAltura() != null ? usuario.getAltura() : 1.70;
            String insulinaLenta = usuario.getInsulinaLenta() != null ? usuario.getInsulinaLenta() : "Basal genérica";
            String insulinaRapida = usuario.getInsulinaRapida() != null ? usuario.getInsulinaRapida() : "Bolus genérica";
            sb.append("2. Utiliza los coeficientes Ratio IC y Factor IS para argumentar médicamente si el paciente podría corregir la tendencia de manera segura basándose en su sensibilidad. ");
            sb.append("Debes considerar obligatoriamente los tiempos de acción de su esquema Basal-Bolus (Lenta: ").append(insulinaLenta).append(", Rápida: ").append(insulinaRapida).append("), ");
            sb.append("así como el peso (").append(peso).append(" kg) y altura (").append(altura).append(" m) al formular tu análisis predictivo o alerta de tendencia.\n");
        } else {
            sb.append("2. Utiliza los coeficientes Ratio IC y Factor IS para argumentar médicamente si el paciente podría corregir la tendencia de manera segura basándose en su sensibilidad.\n");
        }
        sb.append("3. Sé conciso pero riguroso.\n\n");

        sb.append("REQUERIMIENTO DE SALIDA:\n");
        sb.append("Debes responder EXCLUSIVAMENTE en un formato JSON estructurado con las siguientes llaves exactas. No incluyas texto fuera del JSON:\n");
        sb.append("{\n");
        sb.append("  \"riesgo\": \"ALTO\" | \"MEDIO\" | \"BAJO\",\n");
        sb.append("  \"analisis_causal\": \"[Explicación concisa basada en la tendencia, coeficientes clínicos, esquema de insulina Basal-Bolus, peso y altura del paciente]\"\n");
        sb.append("}\n");

        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // MÉTODO NUEVO — Análisis predictivo causal (flujo TimelineAssemblerService)
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * System Prompt de reglas duras que se antepone al payload.
     *
     * DISEÑO ANTI-ALUCINACIÓN:
     *   - Prohíbe explícitamente inventar datos no presentes en la timeline.
     *   - Obliga al razonamiento causal (causa → efecto cronológico).
     *   - Fuerza la salida a JSON puro sin markdown.
     *   - Establece que el LLM es un lector de datos, no un médico que asume.
     */
    public static final String SYSTEM_PROMPT = """
            Eres un motor de análisis clínico predictivo especializado en Diabetes Tipo 1.
            Tu función es EXCLUSIVAMENTE analizar los datos que se te proporcionan en el campo "timeline"\s
            y producir predicciones basadas en patrones detectables en esos datos.
            
            ══ REGLAS ANTI-ALUCINACIÓN (IRROMPIBLES) ══
            
            REGLA 1 — PROHIBICIÓN ABSOLUTA DE SUPOSICIÓN:
            NUNCA asumas, inventes ni estimes un valor de glucosa que NO esté en el array "timeline".
            
            REGLA 2 — RAZONAMIENTO CAUSAL ESTRICTO:
            Cuando existan eventos de tipo MEAL o INSULIN en el timeline, cita el timestamp exacto\s
            del evento y su relación con el cambio de glucosa.
            Ejemplo correcto: "A las 07:30 se consumieron 60g de CH. A las 09:45 la glucosa subió a 198 mg/dL."
            Ejemplo INCORRECTO: "Es probable que el paciente haya comido algo."
            
            REGLA 3 — DIFERENCIACIÓN DE INSULINAS:
            Cuando existan eventos de tipo INSULIN_BASAL o INSULIN_BOLUS en el timeline, distingue su\s
            efecto temporal (BASAL: 12-24h, BOLUS: pico en 1-2h). Si NO existen tales registros,\s
            NO menciones la falta de insulina como causa del problema: ver Regla 6.
            
            REGLA 4 — BRECHAS DE DATOS DE GLUCOSA:
            Si detectas períodos de más de 3 horas sin lecturas de glucosa en el timeline,\s
            regírstralo en "data_quality_notes". NO hagas predicciones sobre períodos sin datos.
            
            REGLA 5 — FORMATO DE SALIDA OBLIGATORIO:
            Tu respuesta debe ser EXCLUSIVAMENTE un objeto JSON válido.\s
            PROHIBIDO incluir texto, explicaciones, markdown (```json), ni comentarios fuera del JSON.\s
            El primer carácter de tu respuesta debe ser '{' y el último '}'.
            
            REGLA 6 — CONTEXTO DE ORÍGEN DE DATOS (CRÍTICA — LEE CON ATENCIÓN):
            El sistema usa LibreLinkUp para extraer lecturas de glucosa de forma AUTOMÁTICA desde\s
            el sensor FreeStyle Libre del paciente. LibreLinkUp SOLO puede capturar glucosa.\s
            Los registros de insulina (INSULIN_BOLUS, INSULIN_BASAL) y de comidas (MEAL) son\s
            entradas MANUALES OPCIONALES que el paciente puede o no haber registrado.\s
            
            POR LO TANTO:
            - La AUSENCIA de registros de insulina NO SIGNIFICA que el paciente no se esté medicando.\s
              Simplemente significa que no los registró manualmente en la aplicación.
            - La AUSENCIA de registros de comidas NO SIGNIFICA que el paciente no haya comido.
            - NUNCA concluyas que el riesgo es CRITICO únicamente por la falta de registros de insulina/comidas.
            - Cuando falten registros manuales, tu análisis debe basarse EXCLUSIVAMENTE en los\s
              patrones de la curva de glucosa: tendencia, variabilidad, nivel promedio, velocidad de cambio.
            - Si el payload indica "has_insulin_records: false" y "has_meal_records: false",\s
              realiza un análisis de tendencia glucométrica pura. El campo "data_quality_notes"\s
              puede mencionar que no hay registros manuales, pero esto NO debe elevar el risk_level.
            
            ══ LIMITACIONES MÉDICAS ══
            No eres un médico. Tus recomendaciones son orientativas y deben ser validadas\s
            por un profesional de la salud antes de ser implementadas por el paciente.
            """;

    /**
     * Construye el payload JSON completo que se envía al LLM para análisis predictivo.
     *
     * El payload tiene tres secciones:
     *   1. {@code patient_profile} — Datos estáticos del paciente (perfil clínico)
     *   2. {@code timeline}        — Lista de eventos cronológicos (causa y efecto)
     *   3. {@code analysis_instructions} — Tarea exacta y schema de respuesta exigido
     *
     * OPTIMIZACIÓN DE TOKENS:
     *   - Las claves de la timeline son abreviadas (ts, type, val) para reducir tokens.
     *   - Los campos null son omitidos por {@code @JsonInclude(NON_NULL)} en TimelineEventDto.
     *   - El perfil del paciente se envía una sola vez, no en cada evento.
     *
     * @param usuario    Paciente. Se usan sus parámetros clínicos y datos demográficos.
     * @param pc         Parámetros clínicos del paciente (puede ser null; se usa fallback del Usuario).
     * @param timeline   Lista de eventos cronológicos generada por TimelineAssemblerService.
     * @param windowDays Número de días que cubre el análisis (ej. 7).
     * @return String con el JSON del payload completo listo para ser enviado a Gemini.
     * @throws IllegalStateException si la serialización JSON falla inesperadamente.
     */
    public static String buildPredictivePayload(
            Usuario usuario,
            ParametroClinico pc,
            List<TimelineEventDto> timeline,
            int windowDays
    ) {
        // ── 1. Resolver parámetros clínicos (pc tiene prioridad sobre usuario) ───
        double isf      = resolveDouble(pc != null ? pc.getFactorSensibilidad()  : null, usuario.getFactorIs(),  50.0);
        double ratioIc  = resolveDouble(pc != null ? pc.getRatioCarbohidratos()  : null, usuario.getRatioIc(),   15.0);
        int    targetMin= pc != null && pc.getObjetivoGlucemiaMin() != null ? pc.getObjetivoGlucemiaMin() : usuario.getRangoGlucosaMin();
        int    targetMax= pc != null && pc.getObjetivoGlucemiaMax() != null ? pc.getObjetivoGlucemiaMax() : usuario.getRangoGlucosaMax();
        int    iobHours = pc != null && pc.getTiempoAccionInsulina() != null ? pc.getTiempoAccionInsulina() : 3;
        double peso     = resolveDouble(usuario.getPesoActual(), null, 74.0);
        double altura   = resolveDouble(usuario.getAltura(),     null, 1.70);
        String basalName= usuario.getInsulinaLenta()  != null && !usuario.getInsulinaLenta().isBlank()  ? usuario.getInsulinaLenta()  : "Lantus";
        String bolusName= usuario.getInsulinaRapida() != null && !usuario.getInsulinaRapida().isBlank() ? usuario.getInsulinaRapida() : "Humalog";

        // ── 2. Calcular edad a partir de fecha de nacimiento ────────────────────
        Integer edad = null;
        if (usuario.getFechaNacimiento() != null) {
            edad = Period.between(usuario.getFechaNacimiento(), LocalDate.now()).getYears();
        }

        // ── 3. Analizar la composición del timeline (qué tipos de datos existen) ───
        long glucoseCount = timeline.stream().filter(e -> "GLUCOSE".equals(e.eventType())).count();
        long insulinCount = timeline.stream().filter(e -> e.eventType() != null && e.eventType().startsWith("INSULIN")).count();
        long mealCount    = timeline.stream().filter(e -> "MEAL".equals(e.eventType())).count();
        boolean hasInsulin = insulinCount > 0;
        boolean hasMeals   = mealCount   > 0;

        // ── 4. Construir el mapa del payload (LinkedHashMap mantiene el orden) ──
        Map<String, Object> payload = new LinkedHashMap<>();

        // — Sección analysis_request —
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("window_days",    windowDays);
        request.put("generated_at",   ZonedDateTime.now(usuario.getZoneId()).toString());
        request.put("timezone",       usuario.getZonaHoraria() != null ? usuario.getZonaHoraria() : "UTC");
        payload.put("analysis_request", request);

        // — Sección data_context —
        Map<String, Object> dataContext = new LinkedHashMap<>();
        dataContext.put("glucose_source",
                "Automático vía LibreLinkUp (sensor FreeStyle Libre). " +
                "Lectura cada 5 minutos. Siempre disponible mientras el sensor esté activo.");
        dataContext.put("insulin_source",
                "Registro MANUAL y OPCIONAL por el paciente en la app. " +
                "La ausencia de registros NO significa que el paciente no se medique.");
        dataContext.put("meal_source",
                "Registro MANUAL y OPCIONAL por el paciente en la app. " +
                "La ausencia de registros NO significa que el paciente no haya comido.");
        dataContext.put("has_glucose_records", glucoseCount > 0);
        dataContext.put("has_insulin_records", hasInsulin);
        dataContext.put("has_meal_records",    hasMeals);
        dataContext.put("event_counts", Map.of(
                "GLUCOSE", glucoseCount,
                "INSULIN", insulinCount,
                "MEAL",    mealCount,
                "TOTAL",   timeline.size()
        ));
        if (!hasInsulin && !hasMeals) {
            dataContext.put("analysis_mode",
                    "SOLO_GLUCOSA: No hay registros manuales de insulina ni comidas en la ventana. " +
                    "Realiza un análisis de TENDENCIA GLUCÉMICA PURA basado en la curva de glucosa: " +
                    "nivel promedio, variabilidad, velocidad de cambio y patrones horarios (mañana/tarde/noche). " +
                    "NO concluyas que el riesgo es CRITICO por falta de registros manuales.");
        } else if (hasInsulin || hasMeals) {
            dataContext.put("analysis_mode",
                    "CAUSAL_PARCIAL: Hay " + insulinCount + " registros de insulina y " + mealCount +
                    " registros de comida. Usa estos eventos para análisis causal cuando estén disponibles. " +
                    "Para los períodos sin registros manuales, aplica análisis de tendencia glucométrica pura.");
        }
        payload.put("data_context", dataContext);

        // — Sección patient_profile —
        Map<String, Object> profile = new LinkedHashMap<>();
        if (edad != null) profile.put("age_years", edad);
        profile.put("weight_kg",  peso);
        profile.put("height_m",   altura);

        Map<String, Object> regimen = new LinkedHashMap<>();
        regimen.put("basal", basalName);
        regimen.put("bolus", bolusName);
        profile.put("insulin_regimen", regimen);

        Map<String, Object> clinicalParams = new LinkedHashMap<>();
        clinicalParams.put("isf_mgdl_per_unit",    isf);
        clinicalParams.put("ic_ratio_g_per_unit",   ratioIc);
        clinicalParams.put("target_range_mgdl",     Map.of("min", targetMin, "max", targetMax));
        clinicalParams.put("insulin_action_hours",  iobHours);
        profile.put("clinical_params", clinicalParams);
        payload.put("patient_profile", profile);

        // — Sección timeline —
        payload.put("timeline", timeline);

        // — Sección analysis_instructions —
        Map<String, Object> instructions = new LinkedHashMap<>();
        instructions.put("task",            "PREDICTIVE_" + windowDays + "DAY");
        instructions.put("output_language", "es");
        
        List<String> focusAreas;
        if (!hasInsulin && !hasMeals) {
            focusAreas = List.of(
                    "glucose_trend_and_variability",
                    "time_in_range_estimation",
                    "nocturnal_hypoglycemia_risk",
                    "hyperglycemia_duration_and_severity",
                    "diurnal_glucose_patterns"
            );
        } else {
            focusAreas = List.of(
                    "meal_to_glucose_correlation",
                    "insulin_bolus_effectiveness",
                    "nocturnal_hypoglycemia_risk",
                    "postprandial_hyperglycemia_patterns",
                    "basal_insulin_stability"
            );
        }
        instructions.put("focus_areas", focusAreas);

        // Schema de respuesta EXACTO que debe producir el LLM
        Map<String, Object> outputSchema = new LinkedHashMap<>();
        outputSchema.put("trend_summary",   "string — Resumen narrativo de la tendencia en la ventana. Si no hay registros manuales, basar en la curva de glucosa exclusivamente.");
        outputSchema.put("risk_level",      "enum: BAJO | MEDIO | ALTO | CRITICO. La ausencia de registros manuales de insulina/comidas NO eleva el riesgo por sí sola.");
        outputSchema.put("predictive_alerts", List.of(Map.of(
                "type",        "string — Ej: HIPOGLUCEMIA_NOCTURNA, HIPERGLUCEMIA_PROLONGADA, VARIABILIDAD_ALTA",
                "probability", "number 0.0 a 1.0 — Probabilidad de ocurrencia en las próximas 4h",
                "description", "string — Descripción del patrón detectado en la curva de glucosa"
        )));
        outputSchema.put("recommendations", "array de strings — Acciones orientativas basadas en los datos disponibles");
        outputSchema.put("data_quality_notes", "array de strings — Mencionar si faltan registros manuales, pero SIN usar esto para elevar el risk_level");
        instructions.put("output_schema", outputSchema);
        payload.put("analysis_instructions", instructions);

        // ── 4. Serializar a JSON ─────────────────────────────────────────────────
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Error serializando el payload predictivo a JSON: " + e.getMessage(), e);
        }
    }

    // ── Helper privado: resuelve el primer valor no-null de una cadena de fallbacks ─
    private static double resolveDouble(Double primary, Double secondary, double defaultValue) {
        if (primary  != null) return primary;
        if (secondary != null) return secondary;
        return defaultValue;
    }
}
