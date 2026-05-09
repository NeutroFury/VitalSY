package com.jonesys.vitalsy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InsulinaResponse {
    
    private Integer id;
    private Integer usuarioId;
    private Double unidadesSugeridas;
    private Double unidadesAplicadas;
    private String tipoInsulina;
    private String sitioAplicacion;
    private Integer glucoseReadingId;
    private Integer registroNutricionId;
    private ZonedDateTime fechaHora;
}
