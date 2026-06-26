-- V10__add_rango_glucosa_to_usuarios.sql

ALTER TABLE usuarios 
ADD COLUMN IF NOT EXISTS rango_glucosa_min INT DEFAULT 70,
ADD COLUMN IF NOT EXISTS rango_glucosa_max INT DEFAULT 180;
