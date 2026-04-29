package com.jonesys.vitalsy.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.ZonedDateTime;

@Entity
@Table(name = "alertas_predicciones")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertaPrediccion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    
    @Column(name = "tipo_alerta", length = 50)
    private String tipoAlerta; // PREDICCION_HIPO, PREDICCION_HIPER
    
    @Column(precision = 3, scale = 2)
    private Double probabilidad; // 0.00 a 1.00
    
    @Column(name = "mensaje_notificacion", columnDefinition = "TEXT")
    private String mensajeNotificacion;
    
    @Column(nullable = false)
    private Boolean leida = false;
    
    @Column(name = "fecha_hora", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private ZonedDateTime fechaHora;
    
    @PrePersist
    protected void onCreate() {
        if (fechaHora == null) {
            fechaHora = ZonedDateTime.now();
        }
    }
}
