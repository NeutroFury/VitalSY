package com.jonesys.vitalsy.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CalculoDosisRequest(
        @JsonProperty("usuarioId")
        @NotNull(message = "El ID de usuario es requerido")
        Integer usuarioId,

        @JsonProperty("nombreComida")
        @NotBlank(message = "El nombre de la comida es requerido")
        String nombreComida,

        @JsonProperty("glicemiaActual")
        @Min(value = 0, message = "La glicemia no puede ser negativa")
        Integer glicemiaActual,

        @JsonProperty("carbohidratos")
        @com.fasterxml.jackson.annotation.JsonAlias("carbohidratosGr")
        @Min(value = 0, message = "Los carbohidratos no pueden ser negativos")
        Double carbohidratos
) {
}
