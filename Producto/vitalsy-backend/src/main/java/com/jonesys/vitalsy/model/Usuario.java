package com.jonesys.vitalsy.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Entity
@Table(name = "usuarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(nullable = false, length = 100)
    private String nombre;
    
    @Column(unique = true, nullable = false, length = 100)
    private String email;
    
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;
    
    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;
    
    @Column(length = 20)
    private String genero;
    
    @Column(name = "peso_actual")
    private Double pesoActual;
    
    @Column(name = "altura")
    private Double altura;
    
    @Column(name = "tipo_insulina", length = 50)
    private String tipoInsulina;
    
    @Column(name = "ratio_ic")
    private Double ratioIc; // Insulin-to-Carb
    
    @Column(name = "factor_is")
    private Double factorIs; // Insulin Sensitivity
    
    @Column(name = "alertas_glucosa")
    private Boolean alertasGlucosa = true;
    
    @Column(name = "recordatorio_comidas")
    private Boolean recordatorioComidas = false;

    @Column(name = "rango_glucosa_min")
    private Integer rangoGlucosaMin = 70;
    
    @Column(name = "rango_glucosa_max")
    private Integer rangoGlucosaMax = 180;

    public Integer getRangoGlucosaMin() {
        return rangoGlucosaMin != null ? rangoGlucosaMin : 70;
    }

    public Integer getRangoGlucosaMax() {
        return rangoGlucosaMax != null ? rangoGlucosaMax : 180;
    }
    
    @Column(length = 20)
    private String rol = "PACIENTE";
    
    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "zona_horaria", length = 50)
    private String zonaHoraria = "America/Santiago";

    public ZoneId getZoneId() {
        try {
            return ZoneId.of(zonaHoraria != null ? zonaHoraria : "America/Santiago");
        } catch (Exception e) {
            return ZoneId.of("UTC");
        }
    }
    
    @Column(name = "creado_en", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private ZonedDateTime creadoEn;
    
    @OneToOne(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private ParametroClinico parametroClinico;
    
    @PrePersist
    protected void onCreate() {
        if (creadoEn == null) {
            creadoEn = ZonedDateTime.now();
        }
    }
}
