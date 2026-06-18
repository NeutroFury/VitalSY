package com.jonesys.vitalsy.dto;

public record EscalaDosisFijaAiDTO(
    String momentoDia,
    int glicemiaMin,
    int glicemiaMax,
    double carbohidratosGr,
    double dosisInsulina
) {}
