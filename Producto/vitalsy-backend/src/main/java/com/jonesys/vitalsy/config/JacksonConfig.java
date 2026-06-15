package com.jonesys.vitalsy.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.InstantDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.ZonedDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Configuración de Jackson para serialización correcta de tipos de fecha/hora de Java 8+.
 *
 * PROBLEMA RESUELTO:
 * Sin esta configuración, Jackson serializa ZonedDateTime como:
 *   - Un array numérico: [2026, 6, 8, 7, 15, 0, 0]  ← ilegible para el LLM
 *   - Un timestamp Unix: 1749374100.000000000         ← pierde zona horaria
 *
 * SOLUCIÓN:
 * Se serializa como ISO-8601 con offset explícito: "2026-06-08T07:15:00-04:00"
 * Esto permite al LLM inferir el momento del día (mañana, tarde, noche) sin ambigüedad.
 */
@Configuration
public class JacksonConfig {

    /**
     * Formato ISO-8601 con offset de zona horaria explícito.
     *
     * Ejemplos de salida:
     *   - Horario de invierno en Santiago: "2026-06-08T07:15:00-04:00"
     *   - UTC (servidor):                  "2026-06-08T11:15:00+00:00"
     */
    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        JavaTimeModule javaTimeModule = new JavaTimeModule();

        // Serializar ZonedDateTime con offset explícito
        javaTimeModule.addSerializer(
                ZonedDateTime.class,
                new ZonedDateTimeSerializer(TIMESTAMP_FORMATTER)
        );

        // Deserializar preservando el offset que envía el cliente Ionic/Capacitor
        // InstantDeserializer.ZONED_DATE_TIME mantiene el ZoneId original del string
        javaTimeModule.addDeserializer(
                ZonedDateTime.class,
                InstantDeserializer.ZONED_DATE_TIME
        );

        return new ObjectMapper()
                .registerModule(javaTimeModule)
                // NUNCA serializar fechas como timestamps Unix o arrays numéricos
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                // No fallar en propiedades JSON desconocidas (compatibilidad hacia adelante)
                .disable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
