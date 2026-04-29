package com.jonesys.vitalsy.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
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
    
    @Column(name = "peso_actual", precision = 5, scale = 2)
    private Double pesoActual;
    
    @Column(length = 20)
    private String rol = "PACIENTE";
    
    @Column(nullable = false)
    private Boolean activo = true;
    
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
