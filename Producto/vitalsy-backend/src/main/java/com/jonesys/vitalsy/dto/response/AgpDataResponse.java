package com.jonesys.vitalsy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgpDataResponse {
    private Integer diasAnalizados;
    private List<AgpPoint> mediana;
    private List<AgpRangePoint> rango50;
    private List<AgpRangePoint> rango90;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgpPoint {
        private String x;
        private double y;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgpRangePoint {
        private String x;
        private double[] y;
    }
}
