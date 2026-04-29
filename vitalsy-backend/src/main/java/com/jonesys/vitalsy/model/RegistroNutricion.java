package com.jonesys.vitalsy.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.ZonedDateTime;

@Entity
@Table(name = "registros_nutricion")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistroNutricion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    
    @Column(name = "descripcion_comida", length = 255)
    private String descripcionComida;
    
    @Column(name = "carbohidratos_gr", precision = 6, scale = 2, nullable = false)
    private Double carbohidratosGr;
    
    @Column(name = "proteinas_gr", precision = 6, scale = 2)
    private Double proteinasGr;
    
    @Column(name = "grasas_gr", precision = 6, scale = 2)
    private Double grasasGr;
    
    @Column(name = "calorias_kcal")
    private Integer caloriasKcal;
    
    @Column(name = "momento_dia", length = 20)
    private String momentoDia; // Desayuno, Almuerzo, Cena, Snack
    
    @Column(name = "estado_animo", length = 50)
    private String estadoAnimo;
    
    @Column(name = "foto_url", length = 255)
    private String fotoUrl;
    
    @Column(name = "fecha_hora", columnDefinition = "TIMESTAMP WITH TIME ZONE", nullable = false)
    private ZonedDateTime fechaHora;
    
    @PrePersist
    protected void onCreate() {
        if (fechaHora == null) {
            fechaHora = ZonedDateTime.now();
        }
    }
}
