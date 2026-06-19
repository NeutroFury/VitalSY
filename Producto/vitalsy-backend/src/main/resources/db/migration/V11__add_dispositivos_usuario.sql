-- ============================================================
-- V11: Módulo de Notificaciones Push — Dispositivos FCM
-- Soporta múltiples tokens FCM por usuario (multi-dispositivo).
-- NO agrega columnas de umbral a `usuarios`: ya existen como
-- rango_glucosa_min y rango_glucosa_max (V2).
-- ============================================================

CREATE TABLE dispositivos_usuario (
    id             SERIAL PRIMARY KEY,
    usuario_id     INT          NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    fcm_token      VARCHAR(255) NOT NULL,
    plataforma     VARCHAR(10)  DEFAULT 'android', -- 'ios' | 'android' | 'web'
    activo         BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_fcm_token UNIQUE (fcm_token)
);

-- Índice para buscar tokens activos de un usuario rápidamente
CREATE INDEX idx_dispositivos_usuario_id ON dispositivos_usuario (usuario_id, activo);
