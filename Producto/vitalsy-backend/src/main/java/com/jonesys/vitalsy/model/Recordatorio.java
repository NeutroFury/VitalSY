package com.jonesys.vitalsy.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalTime;
import java.time.ZonedDateTime;

@Entity
@Table(name = "recordatorios")
@Data
@NoArgsConstructor
public class Recordatorio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TipoRecordatorio tipo;

    @Column(nullable = false)
    private LocalTime hora;

    @Column(name = "dias_repeticion", nullable = false, length = 100)
    private String diasRepeticion;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_creacion", updatable = false)
    private ZonedDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        if (fechaCreacion == null) {
            fechaCreacion = ZonedDateTime.now();
        }
    }
}
