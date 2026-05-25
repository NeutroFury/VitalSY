package com.jonesys.vitalsy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jonesys.vitalsy.dto.response.ChatResponse;
import com.jonesys.vitalsy.dto.response.IaAnalysisResponse;
import com.jonesys.vitalsy.dto.gemini.GeminiRequest;
import com.jonesys.vitalsy.dto.gemini.GeminiResponse;
import com.jonesys.vitalsy.model.ParametroClinico;
import com.jonesys.vitalsy.repository.ParametroClinicoRepository;
import com.jonesys.vitalsy.model.GlucoseReading;
import com.jonesys.vitalsy.model.Usuario;
import com.jonesys.vitalsy.repository.GlucoseReadingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;

@Service
public class IaService {

    private final ParametroClinicoRepository clinicalRepository;

    private static final Logger log = LoggerFactory.getLogger(IaService.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final GlucoseReadingRepository glucoseRepository;

    @Value("${gemini.api.url}")
    private String geminiUrl;

    @Value("${gemini.api.key}")
    private String geminiKey;

    public IaService(GlucoseReadingRepository glucoseRepository, ParametroClinicoRepository clinicalRepository) {
        this.glucoseRepository = glucoseRepository;
        this.clinicalRepository = clinicalRepository;

        
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(30000);

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeader("Content-Type", "application/json")
                .build();
        this.objectMapper = new ObjectMapper()
                .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public IaAnalysisResponse analizarGlucosa(Usuario usuario) {
        log.info("Iniciando análisis de IA para usuario: {}", usuario.getId());
        
        // 1. Obtener lecturas de los últimos 30 días para estadísticas
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime thirtyDaysAgo = now.minusDays(30);
        List<GlucoseReading> last30DaysReadings = glucoseRepository.findByUsuarioAndFechaHoraBetween(usuario, thirtyDaysAgo, now);

        double mean = 0.0;
        double stdDev = 0.0;
        int totalReadings30Days = last30DaysReadings.size();

        if (totalReadings30Days > 0) {
            double sum = 0.0;
            for (GlucoseReading r : last30DaysReadings) {
                sum += r.getValorMgdl();
            }
            mean = sum / totalReadings30Days;

            if (totalReadings30Days > 1) {
                double sumSquaredDiff = 0.0;
                for (GlucoseReading r : last30DaysReadings) {
                    double diff = r.getValorMgdl() - mean;
                    sumSquaredDiff += diff * diff;
                }
                stdDev = Math.sqrt(sumSquaredDiff / (totalReadings30Days - 1));
            }
        }

        // 2. Obtener las últimas 5 lecturas individuales para la línea de tiempo
        List<GlucoseReading> recentReadings = glucoseRepository.findTop20ByUsuarioOrderByFechaHoraDesc(usuario);
        List<GlucoseReading> targetReadings = recentReadings.size() > 5 ? recentReadings.subList(0, 5) : recentReadings;

        if (targetReadings.isEmpty()) {
            log.warn("Usuario {} no tiene lecturas de glucosa.", usuario.getId());
            return new IaAnalysisResponse("ESTABLE", "BAJO", "No hay datos suficientes para análisis causal.");
        }

        double isf;
        double ratioIc;
        // Obtener parámetros clínicos sincronizados desde ParametroClinico
        ParametroClinico pc = clinicalRepository.findByUsuario(usuario).orElse(null);
        if (pc != null) {
            isf = pc.getFactorSensibilidad() != null ? pc.getFactorSensibilidad() : 50.0;
            ratioIc = pc.getRatioCarbohidratos() != null ? pc.getRatioCarbohidratos() : 15.0;
        } else {
            // Fallback a valores por defecto si no existen
            isf = usuario.getFactorIs() != null ? usuario.getFactorIs() : 50.0;
            ratioIc = usuario.getRatioIc() != null ? usuario.getRatioIc() : 15.0;
        }
        double peso = usuario.getPesoActual() != null ? usuario.getPesoActual() : 74.0;
        double altura = usuario.getAltura() != null ? usuario.getAltura() : 1.70;
        String tipoInsulina = usuario.getTipoInsulina() != null && !usuario.getTipoInsulina().isBlank() ? usuario.getTipoInsulina() : "Rápida";

        log.info("Configuración clínica a utilizar - ISF: {} | Ratio IC: {} | Peso: {} | Altura: {} | Tipo Insulina: {}", 
                isf, ratioIc, peso, altura, tipoInsulina);
        log.info("Se encontraron {} lecturas recientes (últimas 5) para la línea de tiempo.", targetReadings.size());

        // Construir historial dinámico cronológico combinando resumen estadístico y lecturas individuales
        StringBuilder history = new StringBuilder();
        history.append("### RESUMEN ESTADÍSTICO DE GLUCEMIA (ÚLTIMOS 30 DÍAS)\n");
        history.append(String.format("- Total de lecturas en 30 días: %d\n", totalReadings30Days));
        if (totalReadings30Days > 0) {
            history.append(String.format("- Promedio (Media): %.1f mg/dL\n", mean));
            history.append(String.format("- Desviación Estándar (Variabilidad): %.1f mg/dL\n\n", stdDev));
        } else {
            history.append("- Sin suficientes lecturas en los últimos 30 días para generar métricas estadísticas.\n\n");
        }

        history.append("### DETALLE DE ÚLTIMAS LECTURAS INDIVIDUALES (Orden cronológico)\n");
        // Iteramos al revés para que el orden sea cronológico (del más antiguo al actual)
        for (int i = targetReadings.size() - 1; i >= 0; i--) {
            GlucoseReading r = targetReadings.get(i);
            long min = Duration.between(r.getFechaHora(), now).toMinutes();
            int chronologicalOrder = targetReadings.size() - i;
            
            String momento = r.getMomento();
            if (momento == null || momento.trim().isEmpty()) {
                momento = "N/A";
            }
            
            String notas = r.getNotas();
            if (notas == null || notas.trim().isEmpty()) {
                notas = "Sin notas";
            }
            
            history.append(String.format("%d. Hace %d min: %d mg/dL (Momento: %s, Nota: '%s', Tendencia: %s)\n", 
                chronologicalOrder, min, r.getValorMgdl(), momento, notas, r.getTendencia()));
        }
        try {
            String basePrompt = "Actúa como un médico endocrinólogo experto en Diabetes Tipo 1. " +
                    "Tu tarea es analizar la tendencia de glucosa de un paciente y darle una recomendación clara, " +
                    "cercana y profesional. Debes explicar qué está pasando metabólicamente, pero usando un lenguaje " +
                    "que cualquier persona pueda entender sin tecnicismos complejos.\n\n" +
                    "### CONTEXTO CLÍNICO DEL PACIENTE\n" +
                    "- Factor de Sensibilidad (ISF): %.1f mg/dL.\n" +
                    "- Ratio Insulina-Carbohidrato (IC): %.1f g.\n" +
                    "- Peso Actual: %.1f kg.\n" +
                    "- Altura: %.2f m.\n" +
                    "- Tipo de Insulina Utilizada: %s.\n" +
                    "- PARÁMETROS CLÍNICOS ESTRICTOS: Considera hipoglucemia por debajo de %d mg/dL e hiperglucemia por encima de %d mg/dL.\n\n" +
                    "%s\n" +
                    "### INSTRUCCIONES DE ACCIÓN\n" +
                    "1. Analiza si la glucosa está subiendo o bajando demasiado rápido.\n" +
                    "2. Explica brevemente la posible causa (ej. 'quizás la dosis de insulina anterior fue muy alta').\n" +
                    "3. Da una recomendación de acción inmediata (qué comer, qué observar o cuándo consultar a urgencias).\n" +
                    "4. Considera obligatoriamente el tiempo de acción esperado para el tipo de insulina utilizado (%s), así como el peso (%.1f kg) y la altura (%.2f m) del paciente al formular cualquier análisis predictivo o alerta de tendencia.\n" +
                    "5. Ignora el estándar médico general de 70-180 mg/dL y basa todo tu análisis, alertas de tendencia y recomendaciones EXCLUSIVAMENTE en los parámetros clínicos estrictos de este paciente.\n\n" +
                    "### FORMATO DE RESPUESTA (IMPORTANTE: MÁXIMO 4 ORACIONES)\n" +
                    "Responde ESTRICTAMENTE con un objeto JSON válido, sin markdown. " +
                    "{\n" +
                    "  \"tendencia\": \"ESTABLE | CAYENDO | SUBIENDO\",\n" +
                    "  \"nivel_de_riesgo\": \"BAJO | MEDIO | ALTO | CRITICO\",\n" +
                    "  \"consejo_breve\": \"Escribe aquí tu explicación cercana. Ejemplo: 'Tu glucosa está bajando rápido porque la última dosis de insulina parece haber sido fuerte. Come 15g de carbohidratos de acción rápida ahora y revisa tu glucosa en 15 minutos para ver si mejora. Si no sube o te sientes mareado, busca ayuda médica de inmediato.'\"\n" +
                    "}\n\n" +
                    "--- REGLAS ESTRICTAS DE FORMATO (CRÍTICO) ---\n" +
                    "1. PROHIBIDO usar párrafos normales. TODA tu respuesta debe estar estructurada obligatoriamente como una lista de viñetas (-).\n" +
                    "2. Cada viñeta debe ser una instrucción corta y directa (máximo 2 oraciones por punto).\n" +
                    "3. Usa negritas (**texto**) para resaltar valores médicos, métricas y acciones críticas de emergencia.";

            String prompt = String.format(basePrompt, 
                    isf, ratioIc, peso, altura, tipoInsulina,
                    usuario.getRangoGlucosaMin(), usuario.getRangoGlucosaMax(), 
                    history.toString(),
                    tipoInsulina, peso, altura);
            log.debug("Prompt final a enviar a Gemini:\n{}", prompt);

            GeminiRequest request = new GeminiRequest(
                    List.of(new GeminiRequest.Content(List.of(new GeminiRequest.Part(prompt)))),
                    new GeminiRequest.GenerationConfig("application/json")
            );

            String targetUri = geminiUrl + "?key=" + geminiKey;
            String rawJson = restClient.post()
                    .uri(targetUri)
                    .body(request)
                    .retrieve()
                    .body(String.class);

            if (rawJson == null || rawJson.isEmpty()) {
                throw new RuntimeException("Gemini no retornó respuesta.");
            }

            // Luego, procesamos ese String manualmente:
            GeminiResponse response = objectMapper.readValue(rawJson, GeminiResponse.class);

            if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
                throw new RuntimeException("Gemini no retornó candidatos de respuesta.");
            }

            String rawResponse = response.candidates().get(0).content().parts().get(0).text();

            // Intentar extraer el JSON que está entre llaves
            String cleanJson;
            int firstCurly = rawResponse.indexOf('{');
            int lastCurly = rawResponse.lastIndexOf('}');
            if (firstCurly != -1 && lastCurly != -1 && firstCurly < lastCurly) {
                cleanJson = rawResponse.substring(firstCurly, lastCurly + 1);
            } else {
                log.warn("Gemini retornó una respuesta que no contiene JSON válido: {}", rawResponse);
                throw new RuntimeException("IA_RESPONSE_NOT_JSON");
            }
            return objectMapper.readValue(cleanJson, IaAnalysisResponse.class);
            
        } catch (Exception e) {
            log.error("IA_ERROR: {}", e.getMessage(), e);
            throw new RuntimeException("IA_SERVER_UNAVAILABLE");
        }
    }

    public ChatResponse chatear(Usuario usuario, String mensajeUsuario) {
        try {
            double isf;
            double ratioIc;
            // Obtener parámetros clínicos sincronizados desde ParametroClinico
            ParametroClinico pcChat = clinicalRepository.findByUsuario(usuario).orElse(null);
            if (pcChat != null) {
                isf = pcChat.getFactorSensibilidad() != null ? pcChat.getFactorSensibilidad() : 50.0;
                ratioIc = pcChat.getRatioCarbohidratos() != null ? pcChat.getRatioCarbohidratos() : 10.0;
            } else {
                // Fallback a valores por defecto
                isf = usuario.getFactorIs() != null ? usuario.getFactorIs() : 50.0;
                ratioIc = usuario.getRatioIc() != null ? usuario.getRatioIc() : 10.0;
            }
            double peso = usuario.getPesoActual() != null ? usuario.getPesoActual() : 74.0;
            double altura = usuario.getAltura() != null ? usuario.getAltura() : 1.70;
            String tipoInsulina = usuario.getTipoInsulina() != null && !usuario.getTipoInsulina().isBlank() ? usuario.getTipoInsulina() : "Rápida";

            List<GlucoseReading> ultimasLecturas = glucoseRepository.findTop5ByUsuarioOrderByFechaHoraDesc(usuario);
            StringBuilder history = new StringBuilder("### HISTORIAL RECIENTE\n");
            
            if (ultimasLecturas.isEmpty()) {
                history.append("No hay lecturas registradas.\n");
            } else {
                for (GlucoseReading r : ultimasLecturas) {
                    history.append(String.format("- %s: %d mg/dL (Tipo: %s)\n", 
                            r.getFechaHora().toString(), r.getValorMgdl(), r.getTipoRegistro()));
                }
            }

            String basePrompt = "Actúa como un médico endocrinólogo experto en Diabetes Tipo 1. " +
                    "Tu tarea es responder a la pregunta o solicitud del paciente de forma clara, " +
                    "cercana y profesional, teniendo en cuenta su contexto clínico actual.\n\n" +
                    "### CONTEXTO CLÍNICO DEL PACIENTE\n" +
                    "- Factor de Sensibilidad (ISF): %.1f mg/dL.\n" +
                    "- Ratio Insulina-Carbohidrato (IC): %.1f g.\n" +
                    "- Peso Actual: %.1f kg.\n" +
                    "- Altura: %.2f m.\n" +
                    "- Tipo de Insulina Utilizada: %s.\n" +
                    "- PARÁMETROS CLÍNICOS ESTRICTOS: Considera hipoglucemia por debajo de %d mg/dL e hiperglucemia por encima de %d mg/dL.\n\n" +
                    "%s\n" +
                    "### PREGUNTA O MENSAJE DEL PACIENTE\n" +
                    "\"%s\"\n\n" +
                    "### FORMATO DE RESPUESTA\n" +
                    "Responde ESTRICTAMENTE con un objeto JSON válido. " +
                    "{\n" +
                    "  \"respuesta\": \"Escribe aquí tu respuesta directa a la inquietud del paciente.\"\n" +
                    "}\n\n" +
                    "--- REGLAS ESTRICTAS DE FORMATO (CRÍTICO) ---\n" +
                    "1. PROHIBIDO usar párrafos normales. TODA tu respuesta debe estar estructurada obligatoriamente como una lista de viñetas (-).\n" +
                    "2. Cada viñeta debe ser una instrucción corta y directa (máximo 2 oraciones por punto).\n" +
                    "3. Usa negritas (**texto**) para resaltar valores médicos, métricas y acciones críticas de emergencia. " +
                    "Considera el tiempo de acción esperado para el tipo de insulina utilizado (%s), así como el peso (%.1f kg) y la altura (%.2f m) del paciente al formular tu análisis, recomendaciones o alertas.\n" +
                    "Ignora el estándar médico general de 70-180 mg/dL y basa todo tu análisis, alertas de tendencia y recomendaciones EXCLUSIVAMENTE en los parámetros clínicos estrictos de este paciente.";

            String prompt = String.format(basePrompt, 
                    isf, ratioIc, peso, altura, tipoInsulina,
                    usuario.getRangoGlucosaMin(), usuario.getRangoGlucosaMax(), 
                    history.toString(), mensajeUsuario,
                    tipoInsulina, peso, altura);
            log.debug("Prompt de Chat final a enviar a Gemini:\n{}", prompt);

            GeminiRequest request = new GeminiRequest(
                    List.of(new GeminiRequest.Content(List.of(new GeminiRequest.Part(prompt)))),
                    new GeminiRequest.GenerationConfig("application/json")
            );

            String targetUri = geminiUrl + "?key=" + geminiKey;
            String rawJson = restClient.post()
                    .uri(targetUri)
                    .body(request)
                    .retrieve()
                    .body(String.class);

            if (rawJson == null || rawJson.isEmpty()) {
                throw new RuntimeException("Gemini no retornó respuesta al chat.");
            }

            GeminiResponse response = objectMapper.readValue(rawJson, GeminiResponse.class);

            if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
                throw new RuntimeException("Gemini no retornó candidatos de chat.");
            }

            String rawResponse = response.candidates().get(0).content().parts().get(0).text();
            
            int firstCurly = rawResponse.indexOf('{');
            int lastCurly = rawResponse.lastIndexOf('}');
            String cleanJson = rawResponse.substring(firstCurly, lastCurly + 1);
            return objectMapper.readValue(cleanJson, ChatResponse.class);
            
        } catch (Exception e) {
            log.error("CHAT_IA_ERROR: {}", e.getMessage(), e);
            throw new RuntimeException("IA_SERVER_UNAVAILABLE");
        }
    }
}