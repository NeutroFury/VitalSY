package com.jonesys.vitalsy.controller;

import com.jonesys.vitalsy.dto.response.GlucoseReadingDto;
import com.jonesys.vitalsy.service.GlucoseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/registros")
@Slf4j
public class RegistroGlucosaController {

    private final GlucoseService glucoseService;

    public RegistroGlucosaController(GlucoseService glucoseService) {
        this.glucoseService = glucoseService;
    }

    /**
     * GET /api/v1/registros/usuario/{usuarioId}/ultimos
     * Devuelve los últimos 10 registros de glucosa de un usuario.
     * Protegido para Administradores.
     */
    @GetMapping("/usuario/{usuarioId}/ultimos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<GlucoseReadingDto>> getUltimosRegistros(@PathVariable Integer usuarioId) {
        log.info("Petición GET para últimos registros del usuario {} iniciada por un ADMIN", usuarioId);
        List<GlucoseReadingDto> response = glucoseService.getUltimosRegistrosAdmin(usuarioId);
        return ResponseEntity.ok(response);
    }
}
