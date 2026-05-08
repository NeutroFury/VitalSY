package com.jonesys.vitalsy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GlucemiaResponse {
    
    private Integer id;
    private Integer usuarioId;
    private Integer valorMgdl;
    private String tipoRegistro;
    private String tendencia;
    private String dispositivoId;
    private String comentarios;
    private String analisisIa;
    private ZonedDateTime fechaHora;
    private ZonedDateTime creadoEn;
}
