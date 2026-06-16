package com.jonesys.vitalsy.controller;

import com.jonesys.vitalsy.dto.request.CalculoDosisRequest;
import com.jonesys.vitalsy.dto.response.CalculoDosisResponse;
import com.jonesys.vitalsy.model.EscalaDosisFija;
import com.jonesys.vitalsy.service.CalculadoraService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/calculadora")
@Tag(name = "Calculadora Híbrida", description = "Endpoints para la calculadora de dosis y digitalización de pautas médicas")
public class CalculadoraController {

    private final CalculadoraService calculadoraService;

    public CalculadoraController(CalculadoraService calculadoraService) {
        this.calculadoraService = calculadoraService;
    }

    @Operation(summary = "Calcular dosis recomendada", description = "Calcula la dosis de insulina usando tabla médica o algoritmo fallback")
    @PostMapping("/calcular")
    public ResponseEntity<CalculoDosisResponse> calcularDosis(@Valid @RequestBody CalculoDosisRequest request) {
        CalculoDosisResponse response = calculadoraService.calcularDosisRecomendada(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Obtener comidas del usuario", description = "Retorna la lista de comidas configuradas para el usuario")
    @GetMapping("/comidas/{usuarioId}")
    public ResponseEntity<List<String>> obtenerComidas(@PathVariable Integer usuarioId) {
        return ResponseEntity.ok(calculadoraService.obtenerComidasDelUsuario(usuarioId));
    }
}
