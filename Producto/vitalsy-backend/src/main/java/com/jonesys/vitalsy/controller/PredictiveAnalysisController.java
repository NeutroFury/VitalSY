package com.jonesys.vitalsy.controller;

import com.jonesys.vitalsy.dto.prediction.PredictiveAnalysisResult;
import com.jonesys.vitalsy.model.GlucoseReading;
import com.jonesys.vitalsy.model.Usuario;
import com.jonesys.vitalsy.repository.GlucoseReadingRepository;
import com.jonesys.vitalsy.repository.UsuarioRepository;
import com.jonesys.vitalsy.service.GeminiInferenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/predict")
@CrossOrigin(origins = "http://localhost:8100")
public class PredictiveAnalysisController {

    private static final Logger log = LoggerFactory.getLogger(PredictiveAnalysisController.class);

    private final UsuarioRepository usuarioRepository;
    private final GlucoseReadingRepository glucoseRepository;
    private final GeminiInferenceService geminiService;

    public PredictiveAnalysisController(UsuarioRepository usuarioRepository,
                                        GlucoseReadingRepository glucoseRepository,
                                        GeminiInferenceService geminiService) {
        this.usuarioRepository = usuarioRepository;
        this.glucoseRepository = glucoseRepository;
        this.geminiService = geminiService;
    }

    @GetMapping("/{usuarioId}")
    public ResponseEntity<?> getPredictiveAnalysis(@PathVariable Integer usuarioId, Authentication authentication) {
        log.info("📋 Petición de Análisis Predictivo Causal recibida para usuarioId: {}", usuarioId);

        // 1. CONTROL DE SEGURIDAD (Autorización para prevenir IDOR)
        // Validamos que el usuario autenticado sea el mismo que solicita sus predicciones (o administrador)
        Usuario usuarioAutenticado = usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado"));

        if (!usuarioAutenticado.getId().equals(usuarioId) && !"ADMIN".equalsIgnoreCase(usuarioAutenticado.getRol())) {
            log.warn("🚨 Acceso denegado: El usuario {} intentó acceder a datos del usuario con ID {}", 
                    usuarioAutenticado.getEmail(), usuarioId);
            return ResponseEntity.status(403).body("Acceso denegado: No tienes permisos para ver datos de otro usuario.");
        }

        // Obtener el perfil clínico del usuario a analizar
        Usuario usuarioAAnalizar = usuarioAutenticado;
        if (!usuarioAutenticado.getId().equals(usuarioId)) {
            usuarioAAnalizar = usuarioRepository.findById(usuarioId)
                    .orElseThrow(() -> new RuntimeException("Usuario a analizar no encontrado"));
        }

        // 2. OBTENER LAS ÚLTIMAS 3 LECTURAS
        List<GlucoseReading> readings = glucoseRepository.findTop20ByUsuarioOrderByFechaHoraDesc(usuarioAAnalizar);
        if (readings.size() < 3) {
            log.warn("⚠️ Datos insuficientes para análisis causal: {} lecturas", readings.size());
            return ResponseEntity.badRequest().body("Se requieren al menos 3 lecturas de glucosa en el sistema para realizar el análisis causal predictivo.");
        }
        
        // Tomar exactamente las primeras 3 (las más recientes)
        List<GlucoseReading> lastThree = readings.subList(0, 3);

        // 3. EJECUTAR INFERENCIA CON EL USUARIO DIRECTAMENTE
        PredictiveAnalysisResult result = geminiService.analyzeCausality(lastThree, usuarioAAnalizar);

        return ResponseEntity.ok(result);
    }
}
