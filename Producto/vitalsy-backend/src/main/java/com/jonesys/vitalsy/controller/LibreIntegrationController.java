package com.jonesys.vitalsy.controller;

import com.jonesys.vitalsy.dto.request.LibreCredentialsRequest;
import com.jonesys.vitalsy.model.LibreLinkUpConfig;
import com.jonesys.vitalsy.model.Usuario;
import com.jonesys.vitalsy.repository.LibreLinkUpConfigRepository;
import com.jonesys.vitalsy.repository.UsuarioRepository;
import com.jonesys.vitalsy.util.EncryptionUtil;
import com.jonesys.vitalsy.service.LibreLinkUpService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/libreview")
public class LibreIntegrationController {

    private final LibreLinkUpConfigRepository libreLinkUpConfigRepository;
    private final EncryptionUtil encryptionUtil;
    private final UsuarioRepository usuarioRepository;
    private final LibreLinkUpService libreLinkUpService;

    public LibreIntegrationController(LibreLinkUpConfigRepository libreLinkUpConfigRepository,
                                      EncryptionUtil encryptionUtil,
                                      UsuarioRepository usuarioRepository,
                                      LibreLinkUpService libreLinkUpService) {
        this.libreLinkUpConfigRepository = libreLinkUpConfigRepository;
        this.encryptionUtil = encryptionUtil;
        this.usuarioRepository = usuarioRepository;
        this.libreLinkUpService = libreLinkUpService;
    }

    @PostMapping("/connect")
    public ResponseEntity<?> connect(@Valid @RequestBody LibreCredentialsRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("message", "Usuario no autenticado"));
        }

        String email = authentication.getName();
        Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);

        if (usuario == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Usuario no encontrado"));
        }

        try {
            // Verificar credenciales directamente con Abbott antes de guardar
            libreLinkUpService.verifyCredentials(request.email(), request.password());

            String encryptedPassword = encryptionUtil.encrypt(request.password());

            LibreLinkUpConfig config = libreLinkUpConfigRepository.findByUsuario(usuario)
                    .orElseGet(LibreLinkUpConfig::new);

            if (config.getId() == null) {
                config.setUsuario(usuario);
            }

            config.setLibreEmail(request.email());
            config.setLibrePassword(encryptedPassword);
            config.setActivo(true);
            
            // Opcional: limpiar el patientId para forzar una nueva detección si cambian de cuenta
            // config.setLibrePatientId(null); 

            libreLinkUpConfigRepository.save(config);

            return ResponseEntity.ok(Map.of(
                    "message", "Configuración de LibreView guardada con éxito",
                    "status", "success"
            ));

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error al validar credenciales Abbott: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "message", "Error al guardar la configuración: " + e.getMessage(),
                    "status", "error"
            ));
        }
    }
}
