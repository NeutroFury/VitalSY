package com.jonesys.vitalsy.controller;

import com.jonesys.vitalsy.model.GlucoseReading;
import com.jonesys.vitalsy.model.Usuario;
import com.jonesys.vitalsy.repository.GlucoseReadingRepository;
import com.jonesys.vitalsy.repository.UsuarioRepository;
import com.jonesys.vitalsy.service.PdfExportService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1/glucosa")
public class GlucoseController {

    private final GlucoseReadingRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final PdfExportService pdfExportService;

    public GlucoseController(GlucoseReadingRepository repository, UsuarioRepository usuarioRepository, PdfExportService pdfExportService) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.pdfExportService = pdfExportService;
    }

    @PostMapping
    public ResponseEntity<GlucoseReadingDto> registrar(@RequestBody GlucoseReading reading, Authentication authentication) {
        System.out.println("DEBUG: Recibida petición para registrar glucosa: " + reading.getValorMgdl());
        try {
            Usuario usuario = usuarioRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            reading.setUsuario(usuario);
            reading.setFechaHora(ZonedDateTime.now());
            reading.setTipoRegistro("MANUAL");
            
            GlucoseReading saved = repository.save(reading);
            System.out.println("DEBUG: Lectura guardada con éxito. ID: " + saved.getId());
            
            GlucoseReadingDto response = new GlucoseReadingDto(
                saved.getId(),
                saved.getValorMgdl(),
                saved.getTendencia(),
                saved.getTipoRegistro(),
                saved.getFechaHora().toString(),
                saved.getAnalisisIa(),
                saved.getCarbohidratos(),
                saved.getComentarios()
            );
            
            return ResponseEntity.status(201).body(response);
        } catch (Exception e) {
            System.err.println("DEBUG ERROR: Fallo al registrar lectura");
            e.printStackTrace();
            throw e;
        }
    }

        @GetMapping("/ultimas")
        public ResponseEntity<List<GlucoseReadingDto>> ultimasLecturas(Authentication authentication) {
        Usuario usuario = usuarioRepository.findByEmail(authentication.getName())
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        List<GlucoseReading> ultimas = repository.findTop20ByUsuarioOrderByFechaHoraDesc(usuario);
        List<GlucoseReading> cronologicas = new ArrayList<>(ultimas);
        Collections.reverse(cronologicas);

        List<GlucoseReadingDto> response = cronologicas.stream()
            .map(reading -> new GlucoseReadingDto(
                reading.getId(),
                reading.getValorMgdl(),
                reading.getTendencia(),
                reading.getTipoRegistro(),
                reading.getFechaHora().toString(),
                reading.getAnalisisIa(),
                reading.getCarbohidratos(),
                reading.getComentarios()
            ))
            .toList();

        return ResponseEntity.ok(response);
        }

        @GetMapping("/historial")
        public ResponseEntity<?> historial(Authentication authentication) {
            System.out.println("DEBUG: Recuperando historial para: " + authentication.getName());
            try {
                Usuario usuario = usuarioRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

                List<GlucoseReading> historial = repository.findByUsuarioOrderByFechaHoraDesc(usuario);
                
                List<GlucoseReadingDto> response = historial.stream()
                    .map(reading -> new GlucoseReadingDto(
                        reading.getId(),
                        reading.getValorMgdl(),
                        reading.getTendencia(),
                        reading.getTipoRegistro(),
                        reading.getFechaHora().toString(),
                        reading.getAnalisisIa(),
                        reading.getCarbohidratos(),
                        reading.getComentarios()
                    ))
                    .toList();

                System.out.println("DEBUG: Historial recuperado. Total registros: " + response.size());
                return ResponseEntity.ok(response);
            } catch (Exception e) {
                System.err.println("DEBUG ERROR: Fallo al recuperar historial");
                e.printStackTrace();
                return ResponseEntity.status(500).body("Error interno: " + e.getMessage());
            }
        }

        @GetMapping("/exportar-pdf")
        public ResponseEntity<InputStreamResource> exportarPdf(Authentication authentication) {
            System.out.println("DEBUG: Exportando PDF para: " + authentication.getName());
            try {
                Usuario usuario = usuarioRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

                List<GlucoseReading> readings = repository.findByUsuarioOrderByFechaHoraDesc(usuario);
                ByteArrayInputStream bis = pdfExportService.generateGlucosePdf(usuario, readings);

                HttpHeaders headers = new HttpHeaders();
                headers.add("Content-Disposition", "attachment; filename=historial_glucemia.pdf");

                return ResponseEntity.ok()
                        .headers(headers)
                        .contentType(MediaType.APPLICATION_PDF)
                        .body(new InputStreamResource(bis));
            } catch (Exception e) {
                System.err.println("DEBUG ERROR: Fallo al exportar PDF");
                e.printStackTrace();
                throw e;
            }
        }

        public record GlucoseReadingDto(
            Integer id,
            Integer valorMgdl,
            String tendencia,
            String tipoRegistro,
            String fechaHora,
            String analisisIa,
            Integer carbohidratos,
            String comentarios
        ) {}
}
