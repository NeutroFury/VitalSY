package com.jonesys.vitalsy.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Entity
@Table(name = "escala_dosis_fija")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EscalaDosisFija {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "nombre_comida_personalizado", nullable = false, length = 100)
    private String nombreComidaPersonalizado;

    @Column(name = "glicemia_min", nullable = false)
    private Integer glicemiaMin;

    @Column(name = "glicemia_max", nullable = false)
    private Integer glicemiaMax;

    @Column(name = "carbohidratos_gr", nullable = false)
    private Double carbohidratosGr;

    @Column(name = "dosis_insulina", nullable = false)
    private Double dosisInsulina;

    @Column(name = "creado_en", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private ZonedDateTime creadoEn;

    @PrePersist
    protected void onCreate() {
        if (creadoEn == null) {
            creadoEn = ZonedDateTime.now();
        }
    }
}
