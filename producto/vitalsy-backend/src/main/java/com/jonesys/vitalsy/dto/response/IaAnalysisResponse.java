package com.jonesys.vitalsy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IaAnalysisResponse {
    private String tendencia;
    private String nivel_de_riesgo;
    private String consejo_breve;
}
