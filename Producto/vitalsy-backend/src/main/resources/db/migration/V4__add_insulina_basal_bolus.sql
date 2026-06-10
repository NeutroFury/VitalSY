-- V4__add_insulina_basal_bolus.sql

-- Eliminar columna antigua
ALTER TABLE usuarios DROP COLUMN IF EXISTS tipo_insulina;

-- Agregar nuevos campos para basal-bolus
ALTER TABLE usuarios ADD COLUMN insulina_lenta VARCHAR(50);
ALTER TABLE usuarios ADD COLUMN insulina_rapida VARCHAR(50);
