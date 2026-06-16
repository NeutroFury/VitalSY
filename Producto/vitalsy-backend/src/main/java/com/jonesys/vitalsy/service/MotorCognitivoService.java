package com.jonesys.vitalsy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.jonesys.vitalsy.dto.gemini.GeminiRequest;
import com.jonesys.vitalsy.dto.gemini.GeminiResponse;
import com.jonesys.vitalsy.dto.response.EscalaDosisFijaAiDTO;
import com.jonesys.vitalsy.model.EscalaDosisFija;
import com.jonesys.vitalsy.model.Usuario;
import com.jonesys.vitalsy.repository.EscalaDosisFijaRepository;
import com.jonesys.vitalsy.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class MotorCognitivoService {

    private static final Logger log = LoggerFactory.getLogger(MotorCognitivoService.class);

    private final EscalaDosisFijaRepository escalaDosisFijaRepository;
    private final UsuarioRepository usuarioRepository;
    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gemini.api.url}")
    private String geminiUrl;

    @Value("${gemini.api.key}")
    private String geminiKey;

    public MotorCognitivoService(EscalaDosisFijaRepository escalaDosisFijaRepository,
                                 UsuarioRepository usuarioRepository) {
        this.escalaDosisFijaRepository = escalaDosisFijaRepository;
        this.usuarioRepository = usuarioRepository;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(10000);
        requestFactory.setReadTimeout(60000);

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Transactional
    public List<EscalaDosisFija> procesarPautaMedica(List<MultipartFile> archivos, Integer usuarioId) throws IOException {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        String instrucciones = """
                Eres un sistema de OCR médico especializado en pautas de insulina.
                El archivo adjunto puede contener UNA o VARIAS tablas de dosis.
                Para CADA tabla que encuentres:
                  1. Lee el TÍTULO de la tabla (ej: "Desayuno", "Once con Bicicleta", "Almuerzo de Paula")
                     -> este es el valor de 'nombreComidaPersonalizado'. Transcríbelo EXACTAMENTE como aparece.
                  2. Extrae TODAS las celdas de la matriz: cada fila es un rango de glicemia,
                     cada columna es una cantidad de carbohidratos.
                  3. Para rangos abiertos superiores (ej. "más de 300" o "301 o más"), usa glicemiaMax: 999.
                  4. Para rangos abiertos inferiores (ej. "menos de 70" o "menor a 70"), usa glicemiaMin: 0.
                Devuelve un arreglo JSON plano con TODOS los registros de TODAS las tablas encontradas. No incluyas comentarios ni bloques markdown extra.
                ESTRUCTURA DEL JSON (obligatorio usar EXACTAMENTE estos nombres de propiedad):
                [
                  {
                    "nombreComidaPersonalizado": "Desayuno",
                    "glicemiaMin": 70,
                    "glicemiaMax": 100,
                    "carbohidratosGr": 15.0,
                    "dosisInsulina": 2.5
                  }
                ]
                """;

        List<EscalaDosisFija> todasLasEntidades = new ArrayList<>();
        escalaDosisFijaRepository.deleteAllByUsuario_Id(usuarioId);

        for (MultipartFile archivo : archivos) {
            String contentType = archivo.getContentType() != null ? archivo.getContentType() : MimeTypeUtils.IMAGE_JPEG_VALUE;
            String base64Data = Base64.getEncoder().encodeToString(archivo.getBytes());

            GeminiRequest request = new GeminiRequest(
                    List.of(new GeminiRequest.Content(List.of(
                            GeminiRequest.Part.textPart(instrucciones),
                            GeminiRequest.Part.inlineDataPart(contentType, base64Data)
                    ))),
                    new GeminiRequest.GenerationConfig("application/json")
            );

            log.info("Enviando archivo {} a Gemini API...", archivo.getOriginalFilename());

            String targetUri = geminiUrl + "?key=" + geminiKey;
            String responseString = restClient.post()
                    .uri(targetUri)
                    .body(request)
                    .retrieve()
                    .body(String.class);
            
            GeminiResponse response = objectMapper.readValue(responseString, GeminiResponse.class);

            if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
                throw new RuntimeException("Gemini no retornó información de la pauta médica.");
            }

            String rawText = response.candidates().get(0).content().parts().get(0).text();
            String cleanJson = cleanMarkdownJson(rawText);

            List<EscalaDosisFijaAiDTO> dtos = objectMapper.readValue(cleanJson, new TypeReference<List<EscalaDosisFijaAiDTO>>() {});

            if (dtos != null && !dtos.isEmpty()) {
                for (EscalaDosisFijaAiDTO dto : dtos) {
                    EscalaDosisFija entidad = new EscalaDosisFija();
                    entidad.setUsuario(usuario);
                    entidad.setNombreComidaPersonalizado(dto.nombreComidaPersonalizado());
                    entidad.setGlicemiaMin(dto.glicemiaMin());
                    entidad.setGlicemiaMax(dto.glicemiaMax());
                    entidad.setCarbohidratosGr(dto.carbohidratosGr());
                    entidad.setDosisInsulina(dto.dosisInsulina());
                    todasLasEntidades.add(entidad);
                }
            }
        }

        if (todasLasEntidades.isEmpty()) {
            throw new RuntimeException("El motor cognitivo no pudo extraer registros válidos de los archivos proporcionados.");
        }

        return escalaDosisFijaRepository.saveAll(todasLasEntidades);
    }

    private String cleanMarkdownJson(String text) {
        if (text == null) return "[]";
        String trimmed = text.trim();
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
