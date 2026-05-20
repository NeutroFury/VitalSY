package com.jonesys.vitalsy.dto.gemini;

import java.util.List;

public record GeminiRequest(List<Content> contents, GenerationConfig generationConfig) {
    public record Content(List<Part> parts) {}
    public record Part(String text) {}
    public record GenerationConfig(String responseMimeType) {}
}
