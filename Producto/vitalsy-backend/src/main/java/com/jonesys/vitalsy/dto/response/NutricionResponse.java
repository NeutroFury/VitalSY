package com.jonesys.vitalsy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NutricionResponse {
    
    private Integer id;
    private Integer usuarioId;
    private String descripcionComida;
    private Double carbohidratosGr;
    private Double proteinasGr;
    private Double grasasGr;
    private Integer caloriasKcal;
    private String momentoDia;
    private String estadoAnimo;
    private String fotoUrl;
    private ZonedDateTime fechaHora;
}
