package com.jonesys.vitalsy.service;

import com.jonesys.vitalsy.dto.request.CalculoDosisRequest;
import com.jonesys.vitalsy.dto.response.CalculoDosisResponse;
import com.jonesys.vitalsy.model.EscalaDosisFija;
import com.jonesys.vitalsy.model.ParametroClinico;
import com.jonesys.vitalsy.model.Usuario;
import com.jonesys.vitalsy.repository.EscalaDosisFijaRepository;
import com.jonesys.vitalsy.repository.ParametroClinicoRepository;
import com.jonesys.vitalsy.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Service
public class CalculadoraService {

    private final EscalaDosisFijaRepository escalaDosisFijaRepository;
    private final ParametroClinicoRepository parametroClinicoRepository;
    private final UsuarioRepository usuarioRepository;

    public CalculadoraService(EscalaDosisFijaRepository escalaDosisFijaRepository,
                              ParametroClinicoRepository parametroClinicoRepository,
                              UsuarioRepository usuarioRepository) {
        this.escalaDosisFijaRepository = escalaDosisFijaRepository;
        this.parametroClinicoRepository = parametroClinicoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public CalculoDosisResponse calcularDosisRecomendada(CalculoDosisRequest request) {
        // 1. Validar usuario
        Usuario usuario = usuarioRepository.findById(request.usuarioId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + request.usuarioId()));

        // 2. Intentar buscar en la tabla médica fija (Escala)
        Optional<EscalaDosisFija> dosisFijaOpt = escalaDosisFijaRepository.buscarDosisPorTabla(
                usuario.getId(),
                request.nombreComida(),
                request.glicemiaActual(),
                request.carbohidratosGr()
        );

        if (dosisFijaOpt.isPresent()) {
            EscalaDosisFija dosisFija = dosisFijaOpt.get();
            return new CalculoDosisResponse(
                    redondear(dosisFija.getDosisInsulina()),
                    "TABLA_MEDICA_FIJA",
                    null
            );
        }

        // 3. Fallback: Lógica Algorítmica Tradicional
        ParametroClinico parametros = parametroClinicoRepository.findByUsuario(usuario)
                .orElseThrow(() -> new com.jonesys.vitalsy.exception.UsuarioSinConfiguracionException(
                        "El usuario no tiene pauta médica ni parámetros clínicos basales configurados para calcular la dosis de insulina."
                ));

        // Dosis = ((Glicemia Actual - Objetivo) / ISF) + (Carbohidratos / Ratio)
        double glicemiaDiff = request.glicemiaActual() - parametros.getObjetivoGlucemiaMax();
        // Si la glicemia está por debajo del objetivo, la corrección es 0 (no restamos insulina de la comida)
        double correccionGlicemia = Math.max(0.0, glicemiaDiff / parametros.getFactorSensibilidad());
        
        double dosisComida = request.carbohidratosGr() / parametros.getRatioCarbohidratos();
        
        double dosisTotal = correccionGlicemia + dosisComida;
        double dosisFinal = Math.max(0.0, dosisTotal);

        return new CalculoDosisResponse(
                redondear(dosisFinal),
                "ALGORITMO_FALLBACK",
                "No se encontró pauta fija exacta. Se utilizó fórmula basada en parámetros clínicos."
        );
    }

    private Double redondear(Double valor) {
        return BigDecimal.valueOf(valor)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    @Transactional(readOnly = true)
    public java.util.List<String> obtenerComidasDelUsuario(Integer usuarioId) {
        return escalaDosisFijaRepository.findComidasByUsuarioId(usuarioId);
    }
}
