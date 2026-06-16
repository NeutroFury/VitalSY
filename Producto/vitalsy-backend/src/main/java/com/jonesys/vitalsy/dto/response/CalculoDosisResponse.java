package com.jonesys.vitalsy.dto.response;

public record CalculoDosisResponse(
        Double dosisSugerida,
        String metodoCalculo,
        String advertencia
) {
}
