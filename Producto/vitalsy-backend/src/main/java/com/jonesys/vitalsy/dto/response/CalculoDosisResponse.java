package com.jonesys.vitalsy.dto.response;

public record CalculoDosisResponse(
        Double dosisCarbohidratos,
        Double dosisGlicemia,
        Double dosisTotal,
        String metodoAplicado
) {
}
