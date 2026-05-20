package com.jonesys.vitalsy.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.ZonedDateTime;

@Entity
@Table(name = "registros_insulina")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistroInsulina {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    
    @Column(name = "unidades_sugeridas")
    private Double unidadesSugeridas;

    @Column(name = "unidades_aplicadas", nullable = false)
    private Double unidadesAplicadas;
    
    @Column(name = "tipo_insulina", length = 50)
    private String tipoInsulina; // Rápida, Basal
    
    @Column(name = "sitio_aplicacion", length = 50)
    private String sitioAplicacion; // Abdomen, Brazo, Muslo
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registro_glucemia_id")
    private GlucoseReading glucoseReading;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registro_nutricion_id")
    private RegistroNutricion registroNutricion;
    
    @Column(name = "fecha_hora", columnDefinition = "TIMESTAMP WITH TIME ZONE", nullable = false)
    private ZonedDateTime fechaHora;
    
    @PrePersist
    protected void onCreate() {
        if (fechaHora == null) {
            fechaHora = ZonedDateTime.now();
        }
    }
}
