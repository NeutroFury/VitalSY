package com.jonesys.vitalsy.dto.response;

public record GlucoseReadingDto(
    Integer id,
    Integer valorMgdl,
    String tendencia,
    String tipoRegistro,
    String fechaHora,
    String analisisIa,
    Integer carbohidratos,
    String comentarios
) {}
