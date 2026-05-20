package com.jonesys.vitalsy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jonesys.vitalsy.dto.response.IaAnalysisResponse;
import com.jonesys.vitalsy.dto.gemini.GeminiRequest;
import com.jonesys.vitalsy.dto.gemini.GeminiResponse;
import com.jonesys.vitalsy.model.GlucoseReading;
import com.jonesys.vitalsy.model.ParametroClinico;
import com.jonesys.vitalsy.model.Usuario;
import com.jonesys.vitalsy.repository.GlucoseReadingRepository;
import com.jonesys.vitalsy.repository.ParametroClinicoRepository;
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

    private static final Logger log = LoggerFactory.getLogger(IaService.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final GlucoseReadingRepository glucoseRepository;
    private final ParametroClinicoRepository clinicalRepository;

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

        // Recuperar perfil clínico o usar valores por defecto
        ParametroClinico params = clinicalRepository.findByUsuario(usuario).orElse(null);
        int isf = 50;
        int ratioIc = 15;
        boolean isDefault = true;

        if (params != null && params.getFactorSensibilidad() != null && params.getRatioCarbohidratos() != null) {
            isf = params.getFactorSensibilidad().intValue();
            ratioIc = params.getRatioCarbohidratos().intValue();
            isDefault = false;
        }

        log.info("Configuración clínica a utilizar - ISF: {} | Ratio IC: {} (Valores por defecto: {})", isf, ratioIc, isDefault);
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
                    "- Factor de Sensibilidad (ISF): %d mg/dL.\n" +
                    "- Ratio Insulina-Carbohidrato (IC): %d g.\n\n" +
                    "%s\n" +
                    "### INSTRUCCIONES DE ACCIÓN\n" +
                    "1. Analiza si la glucosa está subiendo o bajando demasiado rápido.\n" +
                    "2. Explica brevemente la posible causa (ej. 'quizás la dosis de insulina anterior fue muy alta').\n" +
                    "3. Da una recomendación de acción inmediata (qué comer, qué observar o cuándo consultar a urgencias).\n\n" +
                    "### FORMATO DE RESPUESTA (IMPORTANTE: MÁXIMO 4 ORACIONES)\n" +
                    "Responde ESTRICTAMENTE con un objeto JSON válido, sin markdown. " +
                    "{\n" +
                    "  \"tendencia\": \"ESTABLE | CAYENDO | SUBIENDO\",\n" +
                    "  \"nivel_de_riesgo\": \"BAJO | MEDIO | ALTO | CRITICO\",\n" +
                    "  \"consejo_breve\": \"Escribe aquí tu explicación cercana. Ejemplo: 'Tu glucosa está bajando rápido porque la última dosis de insulina parece haber sido fuerte. Come 15g de carbohidratos de acción rápida ahora y revisa tu glucosa en 15 minutos para ver si mejora. Si no sube o te sientes mareado, busca ayuda médica de inmediato.'\"\n" +
                    "}";

            String prompt = String.format(basePrompt, isf, ratioIc, history.toString());
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
            
            // Esta versión busca el primer '{' y el último '}' garantizando que el JSON completo sea extraído
            int firstCurly = rawResponse.indexOf('{');
            int lastCurly = rawResponse.lastIndexOf('}');
            String cleanJson = rawResponse.substring(firstCurly, lastCurly + 1);
            return objectMapper.readValue(cleanJson, IaAnalysisResponse.class);
            
        } catch (Exception e) {
            log.error("IA_ERROR: {}", e.getMessage(), e);
            throw new RuntimeException("IA_SERVER_UNAVAILABLE");
        }
    }
}