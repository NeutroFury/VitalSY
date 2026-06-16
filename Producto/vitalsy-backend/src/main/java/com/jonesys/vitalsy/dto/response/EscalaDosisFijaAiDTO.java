package com.jonesys.vitalsy.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EscalaDosisFijaAiDTO(
        String nombreComidaPersonalizado,
        Integer glicemiaMin,
        Integer glicemiaMax,
        Double carbohidratosGr,
        Double dosisInsulina
) {
}
