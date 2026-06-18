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
                request.carbohidratos()
        );

        if (dosisFijaOpt.isPresent()) {
            EscalaDosisFija dosisFija = dosisFijaOpt.get();
            return new CalculoDosisResponse(
                    0.0,
                    0.0,
                    redondear(dosisFija.getDosisInsulina()),
                    "TABLA_MEDICA_FIJA"
            );
        }

        // 3. Fallback: Lógica Algorítmica Tradicional (Basal-Bolo)
        if (usuario.getRatioIc() == null || usuario.getRatioIc() <= 0 ||
            usuario.getFactorIs() == null || usuario.getFactorIs() <= 0) {
            throw new com.jonesys.vitalsy.exception.UsuarioSinConfiguracionException(
                    "El usuario no tiene pauta médica ni parámetros clínicos configurados para calcular la dosis de insulina."
            );
        }

        // Dosis = ((Glicemia Actual - Objetivo) / ISF) + (Carbohidratos / Ratio)
        double glicemiaActual = request.glicemiaActual() != null ? (double) request.glicemiaActual() : 0.0;
        double objetivoGlicemia = usuario.getGlicemiaObjetivo() != null ? (double) usuario.getGlicemiaObjetivo() : 100.0;
        double factorIs = usuario.getFactorIs() != null ? usuario.getFactorIs() : 1.0;
        double ratioIc = usuario.getRatioIc() != null ? usuario.getRatioIc() : 1.0;
        double carbohidratos = request.carbohidratos() != null ? request.carbohidratos() : 0.0;

        double glicemiaDiff = glicemiaActual - objetivoGlicemia;
        
        // 1. Bolo de Corrección (Dosis por Glicemia)
        double dosisGlicemia = Math.max(0.0, glicemiaDiff / factorIs);
        
        // 2. Bolo de Alimentos (Dosis por Carbohidratos)
        double dosisCarbohidratos = carbohidratos / ratioIc;
        
        // 3. Sumatoria Final Explícita
        double dosisTotal = dosisCarbohidratos + dosisGlicemia;

        return new CalculoDosisResponse(
                redondear(dosisCarbohidratos),
                redondear(dosisGlicemia),
                redondear(dosisTotal),
                "ALGORITMO_BASAL_BOLO"
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
