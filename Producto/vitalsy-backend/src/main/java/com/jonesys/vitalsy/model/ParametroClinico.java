package com.jonesys.vitalsy.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.ZonedDateTime;

@Entity
@Table(name = "parametros_clinicos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParametroClinico {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @OneToOne
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;
    
    @Column(name = "ratio_carbohidratos", nullable = false)
    private Double ratioCarbohidratos; // IC (Insulin-to-Carb ratio)

    @Column(name = "factor_sensibilidad", nullable = false)
    private Double factorSensibilidad; // IS (Insulin Sensitivity Factor)
    
    @Column(name = "objetivo_glucemia_min")
    private Integer objetivoGlucemiaMin = 70;
    
    @Column(name = "objetivo_glucemia_max")
    private Integer objetivoGlucemiaMax = 150;
    
    @Column(name = "tiempo_accion_insulina")
    private Integer tiempoAccionInsulina = 3; // Horas que dura la insulina activa
    
    @Column(name = "unidad_medida", length = 10)
    private String unidadMedida = "mg/dL";
    
    @Column(name = "ultima_actualizacion", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private ZonedDateTime ultimaActualizacion;
    
    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        ultimaActualizacion = ZonedDateTime.now();
    }
}
