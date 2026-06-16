package com.jonesys.vitalsy.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CalculoDosisRequest(
        @NotNull(message = "El ID de usuario es requerido")
        Integer usuarioId,

        @NotBlank(message = "El nombre de la comida es requerido")
        String nombreComida,

        @NotNull(message = "La glicemia actual es requerida")
        @Min(value = 0, message = "La glicemia no puede ser negativa")
        Integer glicemiaActual,

        @NotNull(message = "Los carbohidratos son requeridos")
        @Min(value = 0, message = "Los carbohidratos no pueden ser negativos")
        Double carbohidratosGr
) {
}
