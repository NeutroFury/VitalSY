package com.jonesys.vitalsy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResponse {
    private Integer id;
    private String nombre;
    private String email;
    private Double pesoActual;
    private Double altura;
    private String tipoInsulina;
    private Double ratioIc;
    private Double factorIs;
    private Boolean alertasGlucosa;
    private Boolean recordatorioComidas;
}
