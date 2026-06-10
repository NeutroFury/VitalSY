-- V5__add_rol_usuario.sql
-- Asegura que la columna rol exista con valor por defecto PACIENTE
-- (puede existir si fue creada por Hibernate ddl-auto=update en entornos anteriores)

ALTER TABLE usuarios
    ADD COLUMN IF NOT EXISTS rol VARCHAR(20) NOT NULL DEFAULT 'PACIENTE';

-- Retrocompatibilidad: usuarios ya existentes sin rol quedan como PACIENTE
UPDATE usuarios SET rol = 'PACIENTE' WHERE rol IS NULL OR rol = '';
