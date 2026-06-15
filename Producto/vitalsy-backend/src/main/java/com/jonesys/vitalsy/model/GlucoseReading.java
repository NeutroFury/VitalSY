package com.jonesys.vitalsy.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.ZoneOffset;
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
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Usuario usuario;
    
    @Column(name = "valor_mgdl", nullable = false)
    private Integer valorMgdl; // Se almacena como INT en BD
    
    @Column(name = "tipo_registro", length = 20, nullable = false)
    private String tipoRegistro; // MANUAL, SENSOR_NFC, SENSOR_BLE

    @Column(name = "carbohidratos")
    private Integer carbohidratos; // Gramos de carbohidratos consumidos
    
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

    /**
     * Deriva el momento del día desde la fecha y hora del registro.
     *
     * Las entidades se rehidratan en UTC (vía ZonedDateTimeConverter), por lo
     * que se usa withZoneSameInstant(ZoneOffset.UTC) para obtener la hora local
     * del servidor. En el contexto del LLM, el timestamp ISO-8601 con offset
     * ya porta la zona del paciente; este campo es sólo un hint de legibilidad.
     */
    public String getMomento() {
        if (fechaHora == null) return "DESCONOCIDO";
        int hora = fechaHora.withZoneSameInstant(ZoneOffset.UTC).getHour();
        if (hora >= 6  && hora < 12) return "MAÑANA";
        if (hora >= 12 && hora < 15) return "MEDIODIA";
        if (hora >= 15 && hora < 20) return "TARDE";
        if (hora >= 20)              return "NOCHE";
        return "MADRUGADA";
    }

    public String getNotas() {
        return comentarios;
    }
}