-- V6__add_reset_password_fields.sql
-- Agrega campos para el flujo "Olvidé mi contraseña"
-- resetPasswordToken: token UUID generado al solicitar recuperación
-- resetPasswordExpiresAt: timestamp de expiración del token (15 minutos)

ALTER TABLE usuarios
    ADD COLUMN IF NOT EXISTS reset_password_token VARCHAR(255),
    ADD COLUMN IF NOT EXISTS reset_password_expires_at TIMESTAMP;

-- Índice para búsqueda rápida por token (el servicio consultará por este campo)
CREATE INDEX IF NOT EXISTS idx_usuarios_reset_password_token
    ON usuarios (reset_password_token);
