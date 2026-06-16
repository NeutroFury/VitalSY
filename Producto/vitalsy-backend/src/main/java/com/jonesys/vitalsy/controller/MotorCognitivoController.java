package com.jonesys.vitalsy.controller;

import com.jonesys.vitalsy.model.EscalaDosisFija;
import com.jonesys.vitalsy.model.Usuario;
import com.jonesys.vitalsy.repository.UsuarioRepository;
import com.jonesys.vitalsy.service.MotorCognitivoService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/cognitivo")
public class MotorCognitivoController {

    private final MotorCognitivoService motorCognitivoService;
    private final UsuarioRepository usuarioRepository;

    public MotorCognitivoController(MotorCognitivoService motorCognitivoService,
                                    UsuarioRepository usuarioRepository) {
        this.motorCognitivoService = motorCognitivoService;
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping(value = "/pauta", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> subirPautaMedica(
            @RequestParam("files") List<MultipartFile> files,
            Authentication authentication) {
        
        Usuario usuario = usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        try {
            List<EscalaDosisFija> extraidas = motorCognitivoService.procesarPautaMedica(files, usuario.getId());
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Pauta médica procesada con éxito por Gemini.",
                    "reglasExtraidas", extraidas.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Fallo al procesar la pauta: " + e.getMessage()
            ));
        }
    }
}
