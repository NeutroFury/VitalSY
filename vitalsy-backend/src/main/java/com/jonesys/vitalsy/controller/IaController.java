package com.jonesys.vitalsy.controller;

import com.jonesys.vitalsy.model.GlucoseReading;
import com.jonesys.vitalsy.repository.GlucoseReadingRepository;
import com.jonesys.vitalsy.service.IaService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ia")
public class IaController {

    private final IaService iaService;
    private final GlucoseReadingRepository repository;

    public IaController(IaService iaService, GlucoseReadingRepository repository) {
        this.iaService = iaService;
        this.repository = repository;
    }

    @PostMapping("/analizar/{id}")
    public GlucoseReading procesarLectura(@PathVariable Integer id) {
        // 1. Buscamos la lectura que insertó el DataSeeder
        GlucoseReading lectura = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lectura no encontrada"));

        // 2. Le pedimos a Gemma 4 que la analice
        String analisis = iaService.analizarGlucosa(lectura.getValorMgdl().doubleValue(), lectura.getTendencia());

        // 3. Guardamos el análisis en la base de datos
        lectura.setAnalisisIa(analisis);
        return repository.save(lectura);
    }
}