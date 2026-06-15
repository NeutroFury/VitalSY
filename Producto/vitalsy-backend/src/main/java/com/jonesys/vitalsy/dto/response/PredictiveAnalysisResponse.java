package com.jonesys.vitalsy.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Respuesta estructurada del análisis predictivo basado en ventana de tiempo.
 *
 * Este DTO es el contrato de salida del LLM para el flujo predictivo.
 * Corresponde exactamente al JSON schema que {@code PromptBuilderUtil.buildPredictivePayload}
 * le exige al LLM en la sección {@code analysis_instructions.output_schema}.
 *
 * DIFERENCIA con {@code IaAnalysisResponse}:
 *   - IaAnalysisResponse: análisis descriptivo simple de las últimas 5 lecturas de glucosa.
 *   - PredictiveAnalysisResponse: análisis predictivo causal sobre una ventana de N días,
 *     con alertas tipadas y recomendaciones estructuradas como array.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PredictiveAnalysisResponse(

        /**
         * Resumen narrativo de la tendencia glucémica en la ventana analizada.
         * Ejemplo: "La glucosa muestra una tendencia a subir post-comida sin corrección suficiente."
         */
        @JsonProperty("trend_summary")
        String trendSummary,

        /**
         * Nivel de riesgo global evaluado por el LLM.
         * Valores válidos: BAJO | MEDIO | ALTO | CRITICO
         */
        @JsonProperty("risk_level")
        String riskLevel,

        /**
         * Lista de alertas predictivas detectadas en la línea de tiempo.
         * Cada alerta describe un patrón de riesgo concreto con su probabilidad estimada.
         */
        @JsonProperty("predictive_alerts")
        List<PredictiveAlert> predictiveAlerts,

        /**
         * Lista de recomendaciones clínicas accionables, ordenadas por prioridad.
         * Nunca contiene suposiciones sobre datos que no estén en el timeline.
         */
        @JsonProperty("recommendations")
        List<String> recommendations,

        /**
         * Notas sobre la calidad o completitud de los datos analizados.
         * El LLM las usa para señalar brechas (ej. "No hay registros de insulina en el día 3").
         */
        @JsonProperty("data_quality_notes")
        List<String> dataQualityNotes

) {
    /**
     * Alerta predictiva individual detectada en la línea de tiempo.
     *
     * @param type        Clasificación del riesgo (ej. "HIPOGLUCEMIA_NOCTURNA", "HIPERGLUCEMIA_POST_PRANDIAL")
     * @param probability Probabilidad estimada de ocurrencia en las próximas horas (0.0 a 1.0)
     * @param description Descripción del patrón causal detectado
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PredictiveAlert(
            @JsonProperty("type")        String type,
            @JsonProperty("probability") Double probability,
            @JsonProperty("description") String description
    ) {}
}
