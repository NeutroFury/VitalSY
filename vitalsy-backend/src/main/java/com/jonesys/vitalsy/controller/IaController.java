package com.jonesys.vitalsy.controller;

import com.jonesys.vitalsy.dto.response.IaAnalysisResponse;
import com.jonesys.vitalsy.model.GlucoseReading;
import com.jonesys.vitalsy.model.Usuario;
import com.jonesys.vitalsy.repository.GlucoseReadingRepository;
import com.jonesys.vitalsy.repository.UsuarioRepository;
import com.jonesys.vitalsy.service.IaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ia")
public class IaController {

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

        GlucoseReading lectura = repository.findLatestByUsuario(usuario);
        
        System.out.println("DEBUG: Solicitando análisis para el usuario: " + usuario.getEmail());
        
        if (lectura == null) {
            System.out.println("DEBUG: No se encontró ninguna lectura para este usuario.");
            return ResponseEntity.noContent().build();
        }

        System.out.println("DEBUG: Analizando lectura ID: " + lectura.getId() + " con valor: " + lectura.getValorMgdl());
        
        IaAnalysisResponse analysis = iaService.analizarGlucosa(lectura.getValorMgdl().doubleValue(), lectura.getTendencia());
        
        // Guardamos el consejo breve en la base de datos para historial
        lectura.setAnalisisIa(analysis.getConsejo_breve());
        repository.save(lectura);
        
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
            throw new RuntimeException("No autorizado para analizar esta lectura");
        }

        IaAnalysisResponse analysis = iaService.analizarGlucosa(lectura.getValorMgdl().doubleValue(), lectura.getTendencia());
        
        lectura.setAnalisisIa(analysis.getConsejo_breve());
        repository.save(lectura);
        
        return ResponseEntity.ok(analysis);
    }
}