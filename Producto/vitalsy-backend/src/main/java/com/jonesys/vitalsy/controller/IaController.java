package com.jonesys.vitalsy.controller;

import com.jonesys.vitalsy.dto.response.IaAnalysisResponse;
import com.jonesys.vitalsy.model.GlucoseReading;
import com.jonesys.vitalsy.model.Usuario;
import com.jonesys.vitalsy.repository.GlucoseReadingRepository;
import com.jonesys.vitalsy.repository.UsuarioRepository;
import com.jonesys.vitalsy.service.IaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ia")
public class IaController {

    private static final Logger log = LoggerFactory.getLogger(IaController.class);

    private final IaService iaService;
    private final GlucoseReadingRepository repository;
    private final UsuarioRepository usuarioRepository;

    public IaController(IaService iaService, GlucoseReadingRepository repository, UsuarioRepository usuarioRepository) {
        this.iaService = iaService;
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping("/analizar-ultima")
    public ResponseEntity<IaAnalysisResponse> analizarUltima(Authentication authentication) {
        Usuario usuario = usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        log.info("Petición de análisis IA para usuario: {}", usuario.getEmail());

        IaAnalysisResponse analysis = iaService.analizarGlucosa(usuario);
        
        // Guardar el consejo breve en la última lectura si existe
        GlucoseReading lectura = repository.findTop1ByUsuarioOrderByFechaHoraDesc(usuario);
        if (lectura != null && analysis != null && analysis.getConsejo_breve() != null) {
            lectura.setAnalisisIa(analysis.getConsejo_breve());
            repository.save(lectura);
        }
        
        if (analysis == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(analysis);
    }

    @PostMapping("/analizar/{id}")
    public ResponseEntity<IaAnalysisResponse> procesarLectura(@PathVariable Integer id, Authentication authentication) {
        Usuario usuario = usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        GlucoseReading lectura = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lectura no encontrada"));

        // Verificación de propiedad
        if (!lectura.getUsuario().getId().equals(usuario.getId())) {
            log.warn("Intento de acceso no autorizado a la lectura {} por usuario {}", id, usuario.getEmail());
            throw new RuntimeException("No autorizado para analizar esta lectura");
        }

        IaAnalysisResponse analysis = iaService.analizarGlucosa(usuario);
        
        if (analysis != null && analysis.getConsejo_breve() != null) {
            lectura.setAnalisisIa(analysis.getConsejo_breve());
            repository.save(lectura);
        }
        
        return ResponseEntity.ok(analysis);
    }
}