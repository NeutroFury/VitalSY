package com.jonesys.vitalsy.controller;

import com.jonesys.vitalsy.model.GlucoseReading;
import com.jonesys.vitalsy.model.Usuario;
import com.jonesys.vitalsy.repository.GlucoseReadingRepository;
import com.jonesys.vitalsy.repository.UsuarioRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;

@RestController
@RequestMapping("/api/v1/glucosa")
@CrossOrigin(origins = "http://localhost:8100")
public class GlucoseController {

    private final GlucoseReadingRepository repository;
    private final UsuarioRepository usuarioRepository;

    public GlucoseController(GlucoseReadingRepository repository, UsuarioRepository usuarioRepository) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping("/registrar")
    public ResponseEntity<GlucoseReading> registrar(@RequestBody GlucoseReading reading, Authentication authentication) {
        Usuario usuario = usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        reading.setUsuario(usuario);
        reading.setFechaHora(ZonedDateTime.now());
        reading.setTipoRegistro("MANUAL");
        
        GlucoseReading saved = repository.save(reading);
        return ResponseEntity.ok(saved);
    }
}
