package com.jonesys.vitalsy.controller;

import com.jonesys.vitalsy.model.LibreLinkUpConfig;
import com.jonesys.vitalsy.model.Usuario;
import com.jonesys.vitalsy.repository.LibreLinkUpConfigRepository;
import com.jonesys.vitalsy.repository.UsuarioRepository;
import com.jonesys.vitalsy.service.LibreLinkUpService;
import com.jonesys.vitalsy.util.EncryptionUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/librelinkup")
@CrossOrigin(origins = "http://localhost:8100")
public class LibreLinkUpController {

    private final LibreLinkUpConfigRepository configRepository;
    private final UsuarioRepository usuarioRepository;
    private final LibreLinkUpService service;
    private final EncryptionUtil encryptionUtil;

    public LibreLinkUpController(LibreLinkUpConfigRepository configRepository,
                                  UsuarioRepository usuarioRepository,
                                  LibreLinkUpService service,
                                  EncryptionUtil encryptionUtil) {
        this.configRepository = configRepository;
        this.usuarioRepository = usuarioRepository;
        this.service = service;
        this.encryptionUtil = encryptionUtil;
    }

    @GetMapping("/status")
    public ResponseEntity<?> getStatus(Authentication authentication) {
        Usuario usuario = usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return configRepository.findByUsuario(usuario)
                .map(config -> ResponseEntity.ok(Map.of(
                        "configurado", true,
                        "email", config.getLibreEmail(),
                        "activo", config.getActivo(),
                        "ultimoSync", config.getUltimoSync() != null ? config.getUltimoSync().toString() : "Nunca"
                )))
                .orElse(ResponseEntity.ok(Map.of("configurado", false)));
    }

    @PostMapping("/setup")
    public ResponseEntity<?> setup(@RequestBody Map<String, String> body, Authentication authentication) {
        Usuario usuario = usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        String email = body.get("email");
        String password = body.get("password");

        if (email == null || password == null || email.isBlank() || password.isBlank()) {
            return ResponseEntity.badRequest().body("El email y la contraseña son requeridos.");
        }

        LibreLinkUpConfig config = configRepository.findByUsuario(usuario)
                .orElse(new LibreLinkUpConfig());

        config.setUsuario(usuario);
        config.setLibreEmail(email);
        config.setLibrePassword(encryptionUtil.encrypt(password));
        config.setActivo(true);
        config.setLibrePatientId(null); // Reiniciar para recalcular conexión al login

        configRepository.save(config);

        return ResponseEntity.ok(Map.of("mensaje", "Configuración de LibreLinkUp guardada con éxito."));
    }

    @PostMapping("/sync")
    public ResponseEntity<?> forceSync(Authentication authentication) {
        Usuario usuario = usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        LibreLinkUpConfig config = configRepository.findByUsuario(usuario)
                .orElseThrow(() -> new RuntimeException("LibreLinkUp no configurado para este usuario."));

        try {
            int guardados = service.syncUserReadings(config);
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Sincronización forzada completada.",
                    "nuevosRegistros", guardados
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error sincronizando: " + e.getMessage());
        }
    }

    @DeleteMapping("/disconnect")
    public ResponseEntity<?> disconnect(Authentication authentication) {
        Usuario usuario = usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        configRepository.findByUsuario(usuario).ifPresent(configRepository::delete);

        return ResponseEntity.ok(Map.of("mensaje", "Cuenta de LibreLinkUp desconectada de VitalSY."));
    }
}
