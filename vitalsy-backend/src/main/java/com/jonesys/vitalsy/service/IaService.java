package com.jonesys.vitalsy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jonesys.vitalsy.dto.response.IaAnalysisResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class IaService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public IaService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
        this.objectMapper = new ObjectMapper(); // Instanciación manual
    }

    public IaAnalysisResponse analizarGlucosa(Double valor, String tendencia) {
        try {
            String rawResponse = chatClient.prompt()
                    .system("Responde estrictamente en formato JSON. No incluyas texto explicativo.")
                    .user(String.format("Glucosa: %.1f, Tendencia: %s. Formato JSON: {\"tendencia\":\"...\",\"nivel_de_riesgo\":\"...\",\"consejo_breve\":\"...\"}", valor, tendencia))
                    .call()
                    .content();
            
            System.out.println("IA_RAW_RESPONSE: " + rawResponse);
            
            // Limpiar posibles etiquetas de markdown
            String cleanJson = rawResponse.replaceAll("(?s).*?\\{", "{").replaceAll("(?s)\\}.*", "}");
            
            return objectMapper.readValue(cleanJson, IaAnalysisResponse.class);
            
        } catch (Exception e) {
            System.err.println("IA_ERROR: " + e.getMessage());
            throw new RuntimeException("IA_SERVER_UNAVAILABLE");
        }
    }
}