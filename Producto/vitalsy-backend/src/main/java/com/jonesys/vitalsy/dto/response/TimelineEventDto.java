package com.jonesys.vitalsy.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;

/**
 * Representa un evento unificado dentro de la línea de tiempo cronológica del paciente.
 *
 * Este DTO es el átomo del análisis predictivo: cada registro de glucosa,
 * dosis de insulina o ingesta de comida se convierte en un TimelineEventDto
 * antes de ser enviado al LLM. Al tener todos los eventos en un único tipo,
 * el LLM puede leer la causa (comida/insulina) y el efecto (glucosa) en secuencia.
 *
 * TIPOS DE EVENTO (eventType):
 *   - GLUCOSE       → Lectura de glucemia (sensor o manual)
 *   - INSULIN_BOLUS → Dosis de insulina rápida (pre/post-prandial)
 *   - INSULIN_BASAL → Dosis de insulina lenta (basal/nocturna)
 *   - INSULIN       → Dosis de insulina sin tipo clasificable
 *   - MEAL          → Registro de ingesta nutricional
 *
 * SEMÁNTICA DE CAMPOS NUMÉRICOS POR TIPO:
 *   GLUCOSE:       numericValue = mg/dL,    numericValue2 = null
 *   INSULIN_BOLUS: numericValue = unidades,  numericValue2 = unidades sugeridas
 *   INSULIN_BASAL: numericValue = unidades,  numericValue2 = unidades sugeridas
 *   MEAL:          numericValue = carbs (g), numericValue2 = calorías (kcal)
 *
 * @param timestamp     Momento exacto del evento (ISO-8601 con offset de zona horaria)
 * @param eventType     Categoría del evento (ver TIPOS DE EVENTO)
 * @param numericValue  Valor principal del evento (mg/dL, unidades, gramos CH)
 * @param numericValue2 Valor secundario opcional (unidades sugeridas, kcal)
 * @param label         Etiqueta descriptiva (tendencia del sensor, tipo insulina, descripción comida)
 * @param subtype       Subtipo del evento (MANUAL/SENSOR_NFC, sitio de inyección, momento del día)
 * @param notes         Notas contextuales libres (comentarios, estado de ánimo)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TimelineEventDto(

        @JsonProperty("ts")
        ZonedDateTime timestamp,

        @JsonProperty("type")
        String eventType,

        @JsonProperty("val")
        Double numericValue,

        @JsonProperty("val2")
        Double numericValue2,

        @JsonProperty("label")
        String label,

        @JsonProperty("subtype")
        String subtype,

        @JsonProperty("notes")
        String notes
) {}
