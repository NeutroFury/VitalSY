package com.jonesys.vitalsy.util;

import com.jonesys.vitalsy.model.GlucoseReading;
import com.jonesys.vitalsy.repository.GlucoseReadingRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final GlucoseReadingRepository repository;

    public DataSeeder(GlucoseReadingRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Solo insertamos datos si la tabla está vacía para no duplicar
        if (repository.count() == 0) {
            GlucoseReading lectura1 = new GlucoseReading();
            lectura1.setValorMgdl(110.0);
            lectura1.setTendencia("Stable");
            lectura1.setAnalisisIa("Simulación de lectura normal inicial.");

            GlucoseReading lectura2 = new GlucoseReading();
            lectura2.setValorMgdl(185.5);
            lectura2.setTendencia("Rising");
            lectura2.setAnalisisIa("Simulación de alerta por glucosa alta.");

            repository.saveAll(List.of(lectura1, lectura2));
            System.out.println("✅ DataSeeder: Datos de prueba insertados en PostgreSQL.");
        } else {
            System.out.println("ℹ️ DataSeeder: La base de datos ya contiene registros.");
        }
    }
}
