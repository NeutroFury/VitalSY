package com.jonesys.vitalsy.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.ZonedDateTime;

@Entity
@Table(name = "registros_glucemia")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GlucoseReading {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    
    @Column(name = "valor_mgdl", nullable = false)
    private Integer valorMgdl; // Se almacena como INT en BD
    
    @Column(name = "tipo_registro", length = 20, nullable = false)
    private String tipoRegistro; // MANUAL, SENSOR_NFC, SENSOR_BLE
    
    @Column(length = 20)
    private String tendencia; // TrendType: Stable, Rising, Falling, etc.
    
    @Column(name = "dispositivo_id", length = 100)
    private String dispositivoId; // ID del sensor FreeStyle
    
    @Column(columnDefinition = "TEXT")
    private String comentarios;
    
    @Column(columnDefinition = "TEXT")
    private String analisisIa; // Análisis de IA generado
    
    @Column(name = "fecha_hora", columnDefinition = "TIMESTAMP WITH TIME ZONE", nullable = false)
    private ZonedDateTime fechaHora;
    
    @Column(name = "creado_en", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private ZonedDateTime creadoEn;
    
    @PrePersist
    protected void onCreate() {
        if (creadoEn == null) {
            creadoEn = ZonedDateTime.now();
        }
        if (fechaHora == null) {
            fechaHora = ZonedDateTime.now();
        }
    }
}