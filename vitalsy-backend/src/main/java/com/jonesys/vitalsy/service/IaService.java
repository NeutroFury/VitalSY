package com.jonesys.vitalsy.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class IaService {

    private final ChatClient chatClient;

    public IaService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String analizarGlucosa(Double valor, String tendencia) {
        String prompt = String.format(
                "Actúa como un experto en diabetes. El paciente tiene una glucosa de %.1f mg/dL y la tendencia es '%s'. "
                        +
                        "¿Qué riesgos ves y qué recomendación breve darías? Responde de forma humana.",
                valor, tendencia);

        return chatClient.prompt(prompt).call().content();
    }
}