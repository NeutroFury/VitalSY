package com.jonesys.vitalsy.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.ZonedDateTime;

/**
 * Representa un dispositivo físico registrado para un usuario.
 * Un mismo usuario puede tener múltiples tokens FCM activos
 * (por ejemplo, celular y tablet simultáneamente).
 */
@Entity
@Table(name = "dispositivos_usuario")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DispositivoUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /**
     * Token FCM único por dispositivo. NUNCA exponer en respuestas públicas.
     */
    @Column(name = "fcm_token", nullable = false, length = 255, unique = true)
    private String fcmToken;

    /**
     * Plataforma del dispositivo: 'android', 'ios' o 'web'.
     */
    @Column(length = 10)
    private String plataforma = "android";

    @Column(nullable = false)
    private Boolean activo = true;

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
