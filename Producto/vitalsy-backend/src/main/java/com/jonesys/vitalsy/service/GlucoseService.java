package com.jonesys.vitalsy.service;

import com.jonesys.vitalsy.dto.response.AgpDataResponse;
import com.jonesys.vitalsy.dto.response.GlucoseReadingDto;
import com.jonesys.vitalsy.model.GlucoseReading;
import com.jonesys.vitalsy.model.Usuario;
import com.jonesys.vitalsy.repository.GlucoseReadingRepository;
import com.jonesys.vitalsy.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class GlucoseService {

    private final GlucoseReadingRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final PdfExportService pdfExportService;
    private final AgpStatisticsService agpStatisticsService;

    public GlucoseService(GlucoseReadingRepository repository, 
                          UsuarioRepository usuarioRepository, 
                          PdfExportService pdfExportService, 
                          AgpStatisticsService agpStatisticsService) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.pdfExportService = pdfExportService;
        this.agpStatisticsService = agpStatisticsService;
    }

    public GlucoseReadingDto registrar(GlucoseReading reading, String email) {
        log.debug("Registrando glucosa: {} para el usuario: {}", reading.getValorMgdl(), email);
        Usuario usuario = getUsuarioOrThrow(email);
        
        reading.setUsuario(usuario);
        reading.setFechaHora(ZonedDateTime.now());
        reading.setTipoRegistro("MANUAL");
        
        GlucoseReading saved = repository.save(reading);
        log.debug("Lectura guardada con éxito. ID: {}", saved.getId());
        
        return mapToDto(saved);
    }

    public List<GlucoseReadingDto> getUltimasLecturas(String email) {
        log.debug("Recuperando últimas lecturas para el usuario: {}", email);
        Usuario usuario = getUsuarioOrThrow(email);

        List<GlucoseReading> ultimas = repository.findTop20ByUsuarioOrderByFechaHoraDesc(usuario);
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
