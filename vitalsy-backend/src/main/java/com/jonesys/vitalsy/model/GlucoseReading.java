package com.jonesys.vitalsy.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "lecturas_glucosa") // Coincide con tu imagen de pgAdmin
@Data
public class GlucoseReading {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "valor_mgdl")
    private Double valorMgdl;

    private String tendencia;

    @Column(name = "dispositivo_id")
    private String dispositivoId;

    @Column(name = "analisis_ia", columnDefinition = "TEXT")
    private String analisisIa;

    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro = LocalDateTime.now();
}