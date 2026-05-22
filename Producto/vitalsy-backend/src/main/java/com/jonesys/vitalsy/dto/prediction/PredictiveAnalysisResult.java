package com.jonesys.vitalsy.dto.prediction;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PredictiveAnalysisResult(
    String riesgo,
    @JsonProperty("analisis_causal") String analisisCausal
) {}
