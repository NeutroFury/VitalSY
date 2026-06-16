package com.jonesys.vitalsy.dto.gemini;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

public record GeminiRequest(List<Content> contents, GenerationConfig generationConfig) {
    public record Content(List<Part> parts) {}
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Part(String text, InlineData inlineData) {
        public static Part textPart(String text) {
            return new Part(text, null);
        }
        public static Part inlineDataPart(String mimeType, String data) {
            return new Part(null, new InlineData(mimeType, data));
        }
    }
    
    public record InlineData(String mimeType, String data) {}
    
    public record GenerationConfig(String responseMimeType) {}
}
