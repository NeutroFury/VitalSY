package com.jonesys.vitalsy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jonesys.vitalsy.dto.response.ChatResponse;
import com.jonesys.vitalsy.dto.response.IaAnalysisResponse;
import com.jonesys.vitalsy.dto.response.PredictiveAnalysisResponse;
import com.jonesys.vitalsy.dto.response.TimelineEventDto;
import com.jonesys.vitalsy.dto.gemini.GeminiRequest;
import com.jonesys.vitalsy.dto.gemini.GeminiResponse;
import com.jonesys.vitalsy.model.ParametroClinico;
import com.jonesys.vitalsy.repository.ParametroClinicoRepository;
import com.jonesys.vitalsy.model.GlucoseReading;
import com.jonesys.vitalsy.model.Usuario;
import com.jonesys.vitalsy.repository.GlucoseReadingRepository;
import com.jonesys.vitalsy.util.PromptBuilderUtil;
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
    private final TimelineAssemblerService timelineAssemblerService;

    @Value("${gemini.api.url}")
    private String geminiUrl;

    @Value("${gemini.api.key}")
    private String geminiKey;

    public IaService(
            GlucoseReadingRepository glucoseRepository,
            ParametroClinicoRepository clinicalRepository,
            TimelineAssemblerService timelineAssemblerService
    ) {
        this.glucoseRepository = glucoseRepository;
        this.clinicalRepository = clinicalRepository;
        this.timelineAssemblerService = timelineAssemblerService;

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
        // CRÍTICO: usar la zona horaria del usuario para que el límite del día
        // corresponda a su realidad local y no a UTC del servidor.
        ZonedDateTime now = ZonedDateTime.now(usuario.getZoneId());
        ZonedDateTime thirtyDaysAgo = now.minusDays(30).toLocalDate()
                .atStartOfDay(usuario.getZoneId());
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
        String insulinaLenta = usuario.getInsulinaLenta() != null && !usuario.getInsulinaLenta().isBlank() ? usuario.getInsulinaLenta() : "Lantus (Predeterminada)";
        String insulinaRapida = usuario.getInsulinaRapida() != null && !usuario.getInsulinaRapida().isBlank() ? usuario.getInsulinaRapida() : "Humalog (Predeterminada)";
        String tipoInsulinaPrompt = "Lenta/Basal: " + insulinaLenta + ", Rápida/Bolus: " + insulinaRapida;

        log.info("Configuración clínica - ISF: {} | Ratio IC: {} | Peso: {} | Altura: {} | Insulina Lenta: {} | Insulina Rápida: {}",
                isf, ratioIc, peso, altura, insulinaLenta, insulinaRapida);
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
                    "- Esquema Basal-Bolus (Insulina Lenta/Rápida): %s.\n" +
                    "- PARÁMETROS CLÍNICOS ESTRICTOS: Considera hipoglucemia por debajo de %d mg/dL e hiperglucemia por encima de %d mg/dL.\n\n" +
                    "%s\n" +
                    "### INSTRUCCIONES DE ACCIÓN\n" +
                    "1. Analiza si la glucosa está subiendo o bajando demasiado rápido.\n" +
                    "2. Explica brevemente la posible causa (ej. 'quizás la dosis de insulina anterior fue muy alta').\n" +
                    "3. Da una recomendación de acción inmediata (qué comer, qué observar o cuándo consultar a urgencias).\n" +
                    "4. Considera obligatoriamente los tiempos de acción del esquema Basal-Bolus del paciente (%s), así como el peso (%.1f kg) y la altura (%.2f m) al formular cualquier análisis predictivo o alerta de tendencia.\n" +
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
                    isf, ratioIc, peso, altura, tipoInsulinaPrompt,
                    usuario.getRangoGlucosaMin(), usuario.getRangoGlucosaMax(),
                    history.toString(),
                    tipoInsulinaPrompt, peso, altura);
            log.debug("Prompt final a enviar a Gemini:\n{}", prompt);

            GeminiRequest request = new GeminiRequest(
                    List.of(new GeminiRequest.Content(List.of(GeminiRequest.Part.textPart(prompt)))),
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

            // Obtener el timeline completo de los últimos 7 días
            List<TimelineEventDto> timeline = timelineAssemblerService.buildTimeline(usuario, 7);

            // Construir el payload JSON con el perfil del paciente, timeline y el mensaje
            String payloadJson = PromptBuilderUtil.buildChatPayload(usuario, pcChat, timeline, mensajeUsuario);

            String systemPrompt = "Actúa como VitalSY, tu asistente de Inteligencia Artificial especializado en Diabetes Tipo 1. Eres un compañero empático, cercano y humano. " +
                    "Tu tarea es conversar con el usuario, escuchar sus inquietudes y ayudarle a entender sus patrones de glucosa de forma fluida, natural " +
                    "y conversacional.\n\n" +
                    "--- REGLAS DE CONVERSACIÓN Y SEGURIDAD CLÍNICA (CRÍTICO) ---\n" +
                    "1. ROL DE ASISTENTE, NO MÉDICO: Habla como un asistente inteligente y empático, NO como un médico humano. NUNCA uses frases como 'agendemos una consulta', 'te receto', ni actúes como si fueras su doctor tratante.\n" +
                    "2. CONVERSACIONAL Y EMPÁTICO: Responde en uno o dos párrafos fluidos. Haz que el usuario sienta que habla con un compañero comprensivo y humano que se preocupa por él, sin sonar robótico.\n" +
                    "3. CONTEXTO COMPLETO: Tienes acceso a un JSON con los últimos 7 días de datos cronológicos del usuario (glucosa, insulina, comidas, parámetros). Úsalos para darle retroalimentación valiosa si la pregunta lo amerita.\n" +
                    "4. ANTI-ALUCINACIÓN: Basa tus comentarios EXCLUSIVAMENTE en el contexto clínico y la línea de tiempo provistos. No asumas ni inventes dosis, horarios o síntomas.\n" +
                    "5. SEGURIDAD: Si detectas un riesgo crítico en sus datos (como hiperglucemia persistente severa o caída brusca), sugiérele amablemente que contacte a su médico o busque atención de urgencia.\n" +
                    "6. Usa negritas (**texto**) moderadamente solo para resaltar valores o alertas importantes.\n\n" +
                    "### FORMATO DE SALIDA\n" +
                    "Responde ESTRICTAMENTE con un objeto JSON válido con la siguiente estructura:\n" +
                    "{\n" +
                    "  \"respuesta\": \"Tu respuesta conversacional en párrafos normales aquí.\"\n" +
                    "}";

            log.debug("Enviando Chat Payload a Gemini para usuario {}", usuario.getId());

            GeminiRequest request = new GeminiRequest(
                    List.of(new GeminiRequest.Content(List.of(
                            GeminiRequest.Part.textPart(systemPrompt),
                            GeminiRequest.Part.textPart(payloadJson)
                    ))),
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

    // ─────────────────────────────────────────────────────────────────────────
    // NUEVO FLUJO: Análisis Predictivo Causal basado en ventana de tiempo
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Genera un análisis predictivo causal enviando al LLM la línea de tiempo
     * cronológica completa (glucosa + insulina + comidas) de los últimos {@code windowDays} días.
     *
     * FLUJO:
     *   1. TimelineAssemblerService   → extrae y une los eventos cronológicamente
     *   2. ParametroClinicoRepository → obtiene los parámetros clínicos del paciente
     *   3. PromptBuilderUtil          → construye el JSON payload + System Prompt
     *   4. Gemini API (REST)          → recibe el payload, forzando response_mime_type JSON
     *   5. ObjectMapper               → deserializa la respuesta a PredictiveAnalysisResponse
     *
     * ANTI-ALUCINACIÓN:
     *   El System Prompt (5 reglas duras) se envía como primer Part del Content antes del payload.
     *   La API de Gemini trata el primer Part como contexto de sistema cuando solo hay un Content.
     *
     * @param usuario    Paciente a analizar
     * @param windowDays Número de días de historial a incluir (recomendado: 7)
     * @return Respuesta predictiva estructurada con alertas, risk_level y recomendaciones
     */
    public PredictiveAnalysisResponse generarAnalisisPredictivo(Usuario usuario, int windowDays) {
        log.info("🔮 Iniciando análisis predictivo causal para usuario={}, ventana={} días",
                usuario.getId(), windowDays);

        // ── 1. Obtener la línea de tiempo cronológica unificada ────────────────
        List<TimelineEventDto> timeline = timelineAssemblerService.buildTimeline(usuario, windowDays);

        if (timeline.isEmpty()) {
            log.warn("⚠️ Timeline vacío para usuario={}. Retornando análisis por defecto.", usuario.getId());
            return new PredictiveAnalysisResponse(
                    "Sin datos suficientes para el período analizado.",
                    "BAJO",
                    List.of(),
                    List.of("Registra lecturas de glucosa, dosis de insulina y comidas para obtener un análisis predictivo."),
                    List.of("No se encontraron eventos en la ventana de " + windowDays + " días.")
            );
        }

        log.info("📊 Timeline con {} eventos. Construyendo payload para Gemini.", timeline.size());

        // ── 2. Obtener parámetros clínicos (pc puede ser null; hay fallback en buildPredictivePayload) ─
        ParametroClinico pc = clinicalRepository.findByUsuario(usuario).orElse(null);

        // ── 3. Construir el payload JSON con perfil del paciente + timeline + instrucciones ─
        String payloadJson;
        try {
            payloadJson = PromptBuilderUtil.buildPredictivePayload(usuario, pc, timeline, windowDays);
        } catch (IllegalStateException e) {
            log.error("❌ Error construyendo el payload predictivo: {}", e.getMessage(), e);
            throw new RuntimeException("PREDICTIVE_PAYLOAD_BUILD_ERROR");
        }

        log.debug("📤 Payload predictivo (primeros 500 chars): {}", payloadJson.length() > 500
                ? payloadJson.substring(0, 500) + "..." : payloadJson);

        // ── 4. Enviar a Gemini: System Prompt + Payload como dos Parts en un solo Content ─
        //
        // DISEÑO: La API de Gemini (REST) no tiene un campo "systemInstruction" en la versión
        // básica. Se envían dos Parts en el mismo Content:
        //   Part[0] = System Prompt (reglas anti-alucinación)
        //   Part[1] = Payload JSON con los datos del paciente
        // Gemini los procesa secuencialmente, aplicando las reglas del Part[0] al analizar el Part[1].
        try {
            GeminiRequest request = new GeminiRequest(
                    List.of(new GeminiRequest.Content(
                            List.of(
                                    GeminiRequest.Part.textPart(PromptBuilderUtil.SYSTEM_PROMPT),
                                    GeminiRequest.Part.textPart(payloadJson)
                            )
                    )),
                    new GeminiRequest.GenerationConfig("application/json") // Fuerza JSON puro
            );

            String targetUri = geminiUrl + "?key=" + geminiKey;
            String rawJson = restClient.post()
                    .uri(targetUri)
                    .body(request)
                    .retrieve()
                    .body(String.class);

            if (rawJson == null || rawJson.isBlank()) {
                throw new RuntimeException("Gemini no retornó respuesta al análisis predictivo.");
            }

            // ── 5. Parsear la respuesta de Gemini ─────────────────────────────
            GeminiResponse geminiResponse = objectMapper.readValue(rawJson, GeminiResponse.class);

            if (geminiResponse == null
                    || geminiResponse.candidates() == null
                    || geminiResponse.candidates().isEmpty()) {
                throw new RuntimeException("Gemini no retornó candidatos para el análisis predictivo.");
            }

            String rawText = geminiResponse.candidates().get(0).content().parts().get(0).text();
            log.debug("📥 Respuesta cruda de Gemini (primeros 300 chars): {}",
                    rawText.length() > 300 ? rawText.substring(0, 300) + "..." : rawText);

            // Limpiar posibles bloques markdown residuales (defensa extra aunque response_mime_type=JSON)
            String cleanJson = cleanMarkdownFences(rawText);

            PredictiveAnalysisResponse result = objectMapper.readValue(cleanJson, PredictiveAnalysisResponse.class);
            log.info("✅ Análisis predictivo completado. risk_level={}, alertas={}",
                    result.riskLevel(),
                    result.predictiveAlerts() != null ? result.predictiveAlerts().size() : 0);
            return result;

        } catch (Exception e) {
            log.error("❌ PREDICTIVE_IA_ERROR para usuario={}: {} | Causa: {}",
                    usuario.getId(), e.getMessage(),
                    e.getCause() != null ? e.getCause().getMessage() : "sin causa", e);
            throw new RuntimeException("PREDICTIVE_IA_SERVER_UNAVAILABLE");
        }
    }

    /**
     * Elimina bloques de código markdown (```json ... ```) que Gemini puede incluir
     * aunque se le configure response_mime_type=application/json.
     * Es una defensa adicional que no falla si el JSON ya viene limpio.
     */
    private String cleanMarkdownFences(String text) {
        if (text == null) return "{}";
        String trimmed = text.trim();
        if (trimmed.startsWith("```json")) trimmed = trimmed.substring(7);
        else if (trimmed.startsWith("```"))  trimmed = trimmed.substring(3);
        if (trimmed.endsWith("```")) trimmed = trimmed.substring(0, trimmed.length() - 3);
        // Extraer solo el objeto JSON en caso de texto residual alrededor
        int first = trimmed.indexOf('{');
        int last  = trimmed.lastIndexOf('}');
        if (first != -1 && last != -1 && first < last) {
            return trimmed.substring(first, last + 1);
        }
        return trimmed.trim();
    }
}