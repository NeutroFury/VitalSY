package com.jonesys.vitalsy.service;

import com.jonesys.vitalsy.dto.response.AgpDataResponse;
import com.jonesys.vitalsy.model.GlucoseReading;
import com.jonesys.vitalsy.model.Usuario;
import com.jonesys.vitalsy.repository.GlucoseReadingRepository;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AgpStatisticsService {

    private final GlucoseReadingRepository repository;

    public AgpStatisticsService(GlucoseReadingRepository repository) {
        this.repository = repository;
    }

    public AgpDataResponse getAgpData(Usuario usuario) {
        ZoneId userZone = usuario.getZoneId();
        
        // Calcular ventana de 14 días en la zona del usuario
        ZonedDateTime endDate = ZonedDateTime.now(userZone);
        ZonedDateTime startDate = endDate.minusDays(14);

        // Fetch de lecturas
        List<GlucoseReading> readings = repository.findByUsuarioAndFechaHoraBetween(usuario, startDate, endDate);

        // Contar días distintos con datos
        long diasAnalizados = readings.stream()
                .map(r -> r.getFechaHora().withZoneSameInstant(userZone).toLocalDate())
                .distinct()
                .count();

        List<AgpDataResponse.AgpPoint> medianaList = new ArrayList<>();
        List<AgpDataResponse.AgpRangePoint> rango50List = new ArrayList<>();
        List<AgpDataResponse.AgpRangePoint> rango90List = new ArrayList<>();

        // Si tenemos al menos 3 días, calculamos la estadística
        if (diasAnalizados >= 3) {
            // Agrupar lecturas por la hora del día en la zona horaria del usuario
            Map<Integer, List<GlucoseReading>> groupedByHour = readings.stream()
                    .collect(Collectors.groupingBy(r -> r.getFechaHora().withZoneSameInstant(userZone).getHour()));

            Percentile percentileCalc = new Percentile();

            // Iterar de 0 a 23
            for (int hour = 0; hour < 24; hour++) {
                List<GlucoseReading> hourReadings = groupedByHour.get(hour);
                if (hourReadings != null && !hourReadings.isEmpty()) {
                    double[] values = hourReadings.stream()
                            .mapToDouble(GlucoseReading::getValorMgdl)
                            .toArray();
                    
                    percentileCalc.setData(values);
                    
                    double p10 = Math.round(percentileCalc.evaluate(10.0));
                    double p25 = Math.round(percentileCalc.evaluate(25.0));
                    double p50 = Math.round(percentileCalc.evaluate(50.0));
                    double p75 = Math.round(percentileCalc.evaluate(75.0));
                    double p90 = Math.round(percentileCalc.evaluate(90.0));

                    String xLabel = String.format("%02d:00", hour);

                    medianaList.add(new AgpDataResponse.AgpPoint(xLabel, p50));
                    rango50List.add(new AgpDataResponse.AgpRangePoint(xLabel, new double[]{p25, p75}));
                    rango90List.add(new AgpDataResponse.AgpRangePoint(xLabel, new double[]{p10, p90}));
                }
            }
        }

        return AgpDataResponse.builder()
                .diasAnalizados((int) diasAnalizados)
                .mediana(medianaList)
                .rango50(rango50List)
                .rango90(rango90List)
                .build();
    }
}
