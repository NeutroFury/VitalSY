package com.jonesys.vitalsy.service;

import com.jonesys.vitalsy.dto.response.AgpDataResponse;
import com.jonesys.vitalsy.dto.response.GlucoseReadingDto;
import com.jonesys.vitalsy.model.AlertaPrediccion;
import com.jonesys.vitalsy.model.DispositivoUsuario;
import com.jonesys.vitalsy.model.GlucoseReading;
import com.jonesys.vitalsy.model.Usuario;
import com.jonesys.vitalsy.repository.AlertaPrediccionRepository;
import com.jonesys.vitalsy.repository.DispositivoUsuarioRepository;
import com.jonesys.vitalsy.repository.GlucoseReadingRepository;
import com.jonesys.vitalsy.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class GlucoseService {

    private final GlucoseReadingRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final PdfExportService pdfExportService;
    private final AgpStatisticsService agpStatisticsService;
    private final FcmNotificationService fcmNotificationService;
    private final AlertaPrediccionRepository alertaPrediccionRepository;
    private final DispositivoUsuarioRepository dispositivoUsuarioRepository;

    public GlucoseService(GlucoseReadingRepository repository,
                          UsuarioRepository usuarioRepository,
                          PdfExportService pdfExportService,
                          AgpStatisticsService agpStatisticsService,
                          FcmNotificationService fcmNotificationService,
                          AlertaPrediccionRepository alertaPrediccionRepository,
                          DispositivoUsuarioRepository dispositivoUsuarioRepository) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.pdfExportService = pdfExportService;
        this.agpStatisticsService = agpStatisticsService;
        this.fcmNotificationService = fcmNotificationService;
        this.alertaPrediccionRepository = alertaPrediccionRepository;
        this.dispositivoUsuarioRepository = dispositivoUsuarioRepository;
    }

    public GlucoseReadingDto registrar(GlucoseReading reading, String email) {
        log.debug("Registrando glucosa: {} para el usuario: {}", reading.getValorMgdl(), email);
        Usuario usuario = getUsuarioOrThrow(email);

        reading.setUsuario(usuario);
        reading.setFechaHora(ZonedDateTime.now());
        reading.setTipoRegistro("MANUAL");

        // ── BD primero: el guardado NUNCA puede fallar por culpa de FCM ──
        GlucoseReading saved = repository.save(reading);
        log.debug("Lectura guardada con éxito. ID: {}", saved.getId());

        // ── Post-save: evaluación de alertas (best-effort) ──────────────
        try {
            evaluarAlertas(usuario, saved.getValorMgdl());
        } catch (Exception e) {
            // Un fallo en notificaciones NO debe romper la respuesta al usuario
            log.error("Error inesperado en evaluarAlertas para usuario {}: {}", email, e.getMessage());
        }

        return mapToDto(saved);
    }

    /**
     * Evalúa si el valor de glucosa supera los umbrales del usuario.
     * En caso de alerta: persiste en alertas_predicciones Y envía push FCM.
     * Todo el método opera en modo best-effort (nunca propaga excepción).
     */
    private void evaluarAlertas(Usuario usuario, int valorMgdl) {
        // Verificar que el usuario tiene alertas activadas
        if (Boolean.FALSE.equals(usuario.getAlertasGlucosa())) {
            log.debug("Alertas desactivadas para usuario {}. Evaluación omitida.", usuario.getEmail());
            return;
        }

        int min = usuario.getRangoGlucosaMin(); // default: 70
        int max = usuario.getRangoGlucosaMax(); // default: 180

        String tipoAlerta = null;
        String mensaje = null;

        if (valorMgdl <= min) {
            tipoAlerta = "ALERTA_HIPOGLICEMIA";
            mensaje = buildMensajeAlerta("HIPOGLICEMIA", valorMgdl, min, max);
        } else if (valorMgdl >= max) {
            tipoAlerta = "ALERTA_HIPERGLICEMIA";
            mensaje = buildMensajeAlerta("HIPERGLICEMIA", valorMgdl, min, max);
        }

        if (tipoAlerta == null) {
            log.debug("Glucosa {} mg/dL dentro del rango [{}-{}]. Sin alerta.", valorMgdl, min, max);
            return;
        }

        log.info("⚠️  Alerta {} detectada: {} mg/dL para usuario {}", tipoAlerta, valorMgdl, usuario.getEmail());

        // 1. Persistir alerta en BD para historial y auditoría clínica
        AlertaPrediccion alerta = new AlertaPrediccion();
        alerta.setUsuario(usuario);
        alerta.setTipoAlerta(tipoAlerta);
        alerta.setMensajeNotificacion(mensaje);
        alerta.setLeida(false);
        alertaPrediccionRepository.save(alerta);
        log.debug("Alerta persistida en BD para usuario {}.", usuario.getEmail());

        // 2. Enviar push FCM a todos los dispositivos activos (multi-dispositivo)
        List<String> tokens = dispositivoUsuarioRepository
                .findByUsuarioAndActivoTrue(usuario)
                .stream()
                .map(DispositivoUsuario::getFcmToken)
                .toList();

        String titulo = tipoAlerta.contains("HIPO") ? "⚠️ Hipoglicemia detectada" : "🔴 Hiperglicemia detectada";
        Map<String, String> data = Map.of(
                "tipo", tipoAlerta,
                "valor", String.valueOf(valorMgdl),
                "screen", "dashboard"
        );
        fcmNotificationService.enviarAlerta(tokens, titulo, mensaje, data);
    }

    private String buildMensajeAlerta(String tipo, int valor, int min, int max) {
        if ("HIPOGLICEMIA".equals(tipo)) {
            return String.format(
                "Tu glucosa es %d mg/dL, por debajo del límite mínimo de %d mg/dL. Actúa de inmediato.",
                valor, min
            );
        } else {
            return String.format(
                "Tu glucosa es %d mg/dL, por encima del límite máximo de %d mg/dL. Revisa tu plan de insulina.",
                valor, max
            );
        }
    }

    public List<GlucoseReadingDto> getUltimasLecturas(String email) {
        log.debug("Recuperando últimas lecturas para el usuario: {}", email);
        Usuario usuario = getUsuarioOrThrow(email);

        List<GlucoseReading> ultimas = repository.findTop20ByUsuarioOrderByFechaHoraDesc(usuario);
        List<GlucoseReading> cronologicas = new ArrayList<>(ultimas);
        Collections.reverse(cronologicas);

        return cronologicas.stream().map(this::mapToDto).toList();
    }

    public List<GlucoseReadingDto> getUltimosRegistrosAdmin(Integer usuarioId) {
        log.debug("Administrador solicitando últimos 10 registros para el usuario ID: {}", usuarioId);
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        List<GlucoseReading> ultimas = repository.findTop10ByUsuarioOrderByFechaHoraDesc(usuario);
        List<GlucoseReading> cronologicas = new ArrayList<>(ultimas);
        Collections.reverse(cronologicas);

        return cronologicas.stream().map(this::mapToDto).toList();
    }

    public List<GlucoseReadingDto> getHistorial(String email) {
        log.debug("Recuperando historial para el usuario: {}", email);
        Usuario usuario = getUsuarioOrThrow(email);

        List<GlucoseReading> historial = repository.findByUsuarioOrderByFechaHoraDesc(usuario);
        log.debug("Historial recuperado. Total registros: {}", historial.size());
        
        return historial.stream().map(this::mapToDto).toList();
    }

    public ByteArrayInputStream exportarPdf(String email) {
        log.debug("Exportando PDF para el usuario: {}", email);
        Usuario usuario = getUsuarioOrThrow(email);

        List<GlucoseReading> readings = repository.findByUsuarioOrderByFechaHoraDesc(usuario);
        return pdfExportService.generateGlucosePdf(usuario, readings, usuario.getZoneId());
    }

    public AgpDataResponse obtenerAgp(String email) {
        log.debug("Calculando AGP para el usuario: {}", email);
        Usuario usuario = getUsuarioOrThrow(email);
        return agpStatisticsService.getAgpData(usuario);
    }

    private Usuario getUsuarioOrThrow(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    private GlucoseReadingDto mapToDto(GlucoseReading reading) {
        return new GlucoseReadingDto(
            reading.getId(),
            reading.getValorMgdl(),
            reading.getTendencia(),
            reading.getTipoRegistro(),
            reading.getFechaHora().toString(),
            reading.getAnalisisIa(),
            reading.getCarbohidratos(),
            reading.getComentarios()
        );
    }
}
