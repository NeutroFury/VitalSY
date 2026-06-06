package com.jonesys.vitalsy.controller;

import com.jonesys.vitalsy.dto.response.UsuarioResponse;
import com.jonesys.vitalsy.dto.request.ParametrosClinicosDTO;
import com.jonesys.vitalsy.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/usuarios")
@Slf4j
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/perfil")
    public ResponseEntity<UsuarioResponse> getPerfil(Authentication authentication) {
        log.info("Petición GET perfil para: {}", authentication.getName());
        UsuarioResponse response = usuarioService.getPerfil(authentication.getName());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/perfil")
    public ResponseEntity<UsuarioResponse> updatePerfil(@RequestBody UsuarioResponse request, Authentication authentication) {
        log.info("Petición PUT perfil para: {}", authentication.getName());
        log.debug("DATOS RECIBIDOS: {}", request);
        UsuarioResponse response = usuarioService.updatePerfil(authentication.getName(), request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/parametros-clinicos")
    public ResponseEntity<UsuarioResponse> updateParametrosClinicos(@Valid @RequestBody ParametrosClinicosDTO request, Authentication authentication) {
        log.info("Petición PUT parametros-clinicos para: {}", authentication.getName());
        UsuarioResponse response = usuarioService.updateParametrosClinicos(authentication.getName(), request);
        return ResponseEntity.ok(response);
    }
}
