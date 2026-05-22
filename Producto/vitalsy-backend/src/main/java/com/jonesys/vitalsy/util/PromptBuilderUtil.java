package com.jonesys.vitalsy.util;

import com.jonesys.vitalsy.model.GlucoseReading;
import com.jonesys.vitalsy.model.ParametroClinico;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;

public class PromptBuilderUtil {

    public static String buildPrompt(List<GlucoseReading> readings, ParametroClinico params) {
        StringBuilder sb = new StringBuilder();
        sb.append("Actúa como un Endocrinólogo y Experto en Diabetes de clase mundial.\n");
        sb.append("Analiza la causalidad de la tendencia de glucosa del paciente de forma anónima.\n\n");
        
        sb.append("DATOS CLÍNICOS DEL PACIENTE:\n");
        sb.append("- Coeficientes Médicos:\n");
        if (params != null) {
            sb.append("  * Ratio IC (Relación Insulina-Carbohidratos): ").append(params.getRatioCarbohidratos()).append("\n");
            sb.append("  * Factor IS (Factor de Sensibilidad a la Insulina): ").append(params.getFactorSensibilidad()).append(" mg/dL por unidad\n");
        } else {
            sb.append("  * Ratio IC: No configurado (usar estándar estimado)\n");
            sb.append("  * Factor IS: No configurado (usar estándar estimado)\n");
        }
        
        sb.append("- Historial de las Últimas 3 Lecturas (de la más reciente a la más antigua):\n");
        ZonedDateTime ahora = ZonedDateTime.now();
        for (int i = 0; i < readings.size(); i++) {
            GlucoseReading r = readings.get(i);
            long minutosAtras = Duration.between(r.getFechaHora(), ahora).toMinutes();
            sb.append(String.format("  * Lectura %d: %d mg/dL (hace %d minutos), Tendencia: %s, Origen: %s\n", 
                    i + 1, r.getValorMgdl(), minutosAtras, r.getTendencia() != null ? r.getTendencia() : "Estable", r.getTipoRegistro()));
        }

        sb.append("\nINSTRUCCIÓN DE ANÁLISIS:\n");
        sb.append("1. Analiza si la velocidad de cambio de glucemia presenta un riesgo inminente en los próximos 60 a 120 minutos ");
        sb.append("de cruzar los 60 mg/dL (hipoglucemia severa) o los 250 mg/dL (hiperglucemia severa).\n");
        sb.append("2. Utiliza los coeficientes Ratio IC y Factor IS para argumentar médicamente si el paciente podría corregir la tendencia de manera segura basándose en su sensibilidad.\n");
        sb.append("3. Sé conciso pero riguroso.\n\n");

        sb.append("REQUERIMIENTO DE SALIDA:\n");
        sb.append("Debes responder EXCLUSIVAMENTE en un formato JSON estructurado con las siguientes llaves exactas. No incluyas texto fuera del JSON:\n");
        sb.append("{\n");
        sb.append("  \"riesgo\": \"ALTO\" | \"MEDIO\" | \"BAJO\",\n");
        sb.append("  \"analisis_causal\": \"[Explicación concisa basada en la tendencia y coeficientes clínicos]\"\n");
        sb.append("}\n");

        return sb.toString();
    }
}
