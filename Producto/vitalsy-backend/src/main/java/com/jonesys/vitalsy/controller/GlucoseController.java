package com.jonesys.vitalsy.controller;

import com.jonesys.vitalsy.dto.response.AgpDataResponse;
import com.jonesys.vitalsy.dto.response.GlucoseReadingDto;
import com.jonesys.vitalsy.model.GlucoseReading;
import com.jonesys.vitalsy.service.GlucoseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.util.List;

@RestController
@RequestMapping("/api/v1/glucosa")
@Slf4j
public class GlucoseController {

    private final GlucoseService glucoseService;

    public GlucoseController(GlucoseService glucoseService) {
        this.glucoseService = glucoseService;
    }

    @PostMapping
    public ResponseEntity<GlucoseReadingDto> registrar(@RequestBody GlucoseReading reading, Authentication authentication) {
        log.info("Petición POST para registrar glucosa iniciada por el usuario: {}", authentication.getName());
        GlucoseReadingDto response = glucoseService.registrar(reading, authentication.getName());
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/ultimas")
    public ResponseEntity<List<GlucoseReadingDto>> ultimasLecturas(Authentication authentication) {
        log.info("Petición GET para ultimas lecturas iniciada por el usuario: {}", authentication.getName());
        List<GlucoseReadingDto> response = glucoseService.getUltimasLecturas(authentication.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/historial")
    public ResponseEntity<List<GlucoseReadingDto>> historial(Authentication authentication) {
        log.info("Petición GET para historial iniciada por el usuario: {}", authentication.getName());
        List<GlucoseReadingDto> response = glucoseService.getHistorial(authentication.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/exportar-pdf")
    public ResponseEntity<InputStreamResource> exportarPdf(Authentication authentication) {
        log.info("Petición GET para exportar PDF iniciada por el usuario: {}", authentication.getName());
        ByteArrayInputStream bis = glucoseService.exportarPdf(authentication.getName());

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=historial_glucemia.pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(bis));
    }

    @GetMapping("/agp")
    public ResponseEntity<AgpDataResponse> obtenerAgp(Authentication authentication) {
        log.info("Petición GET para obtener AGP iniciada por el usuario: {}", authentication.getName());
        AgpDataResponse response = glucoseService.obtenerAgp(authentication.getName());
        return ResponseEntity.ok(response);
    }
}
