package com.jonesys.vitalsy.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.ZonedDateTime;

@Entity
@Table(name = "librelinkup_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LibreLinkUpConfig {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;
    
    @Column(name = "libre_email", nullable = false, length = 255)
    private String libreEmail;
    
    @Column(name = "libre_password", nullable = false, length = 255)
    private String librePassword;
    
    @Column(name = "libre_patient_id", length = 100)
    private String librePatientId;
    
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;
    
    @Column(name = "ultimo_sync", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private ZonedDateTime ultimoSync;
    
    @Column(name = "creado_en", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private ZonedDateTime creadoEn;

    @Column(name = "actualizado_en", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private ZonedDateTime actualizadoEn;
    
    @PrePersist
    protected void onCreate() {
        creadoEn = ZonedDateTime.now();
        actualizadoEn = ZonedDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        actualizadoEn = ZonedDateTime.now();
    }
}
