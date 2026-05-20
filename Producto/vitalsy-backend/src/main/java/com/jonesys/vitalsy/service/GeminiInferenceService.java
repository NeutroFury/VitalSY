package com.jonesys.vitalsy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jonesys.vitalsy.dto.gemini.*;
import com.jonesys.vitalsy.dto.prediction.PredictiveAnalysisResult;
import com.jonesys.vitalsy.model.GlucoseReading;
import com.jonesys.vitalsy.model.ParametroClinico;
import com.jonesys.vitalsy.util.PromptBuilderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.List;

@Service
public class GeminiInferenceService {

    private static final Logger log = LoggerFactory.getLogger(GeminiInferenceService.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gemini.api.url}")
    private String geminiUrl;

    @Value("${gemini.api.key}")
    private String geminiKey;

    public GeminiInferenceService() {
        
        // Configurar Timeouts estrictos (30 segundos de lectura, 5 de conexión)
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(30000);

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public PredictiveAnalysisResult analyzeCausality(List<GlucoseReading> readings, ParametroClinico params) {
        log.info("🤖 Iniciando inferencia con Gemini API (Privacy-safe)...");
        try {
            // 1. Construir prompt anónimo
            String prompt = PromptBuilderUtil.buildPrompt(readings, params);
            
            // 2. Preparar el DTO de Petición
            GeminiRequest request = new GeminiRequest(
                    List.of(new GeminiRequest.Content(List.of(new GeminiRequest.Part(prompt)))),
                    new GeminiRequest.GenerationConfig("application/json") // Fuerza formato JSON en Gemini 1.5+
            );

            // 3. Realizar consulta REST
            String targetUri = geminiUrl + "?key=" + geminiKey;
            GeminiResponse response = restClient.post()
                    .uri(targetUri)
                    .body(request)
                    .retrieve()
                    .body(GeminiResponse.class);

            if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
                throw new RuntimeException("Gemini no retornó candidatos de respuesta.");
            }

            // 4. Extraer y limpiar JSON generado
            String rawText = response.candidates().get(0).content().parts().get(0).text();
            log.debug("Gemini raw text: {}", rawText);

            String cleanJson = cleanMarkdownJson(rawText);
            
            // 5. Deserializar respuesta
            return objectMapper.readValue(cleanJson, PredictiveAnalysisResult.class);

        } catch (Exception e) {
            log.error("❌ Fallo en el análisis predictivo causal con Gemini: {}", e.getMessage(), e);
            // Fallback elegante en caso de fallos
            return new PredictiveAnalysisResult(
                    "MEDIO", 
                    "Análisis predictivo temporalmente limitado debido a indisponibilidad de la IA externa. Por favor, monitorice sus sensaciones clínicas convencionales."
            );
        }
    }

    private String cleanMarkdownJson(String text) {
        if (text == null) return "{}";
        String trimmed = text.trim();
        // Quitar bloques markdown de tipo ```json o ``` si Gemini los incluye por inercia
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }
}
