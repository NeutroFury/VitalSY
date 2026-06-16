-- 1. Limpiar datos incompatibles con el nuevo esquema
TRUNCATE TABLE escala_dosis_fija;

-- 2. Eliminar constraint restrictivo
ALTER TABLE escala_dosis_fija DROP CONSTRAINT IF EXISTS chk_momento_dia;

-- 3. Renombrar columna y ampliar tamaño
ALTER TABLE escala_dosis_fija RENAME COLUMN momento_dia TO nombre_comida_personalizado;
ALTER TABLE escala_dosis_fija ALTER COLUMN nombre_comida_personalizado TYPE VARCHAR(100);

-- 4. Agregar columna de timestamping (si no existe de sesiones anteriores)
ALTER TABLE escala_dosis_fija ADD COLUMN IF NOT EXISTS creado_en TIMESTAMPTZ DEFAULT NOW();

-- 5. Recrear índice optimizado para la query híbrida
DROP INDEX IF EXISTS idx_escala_calculo_hibrido;
CREATE INDEX idx_escala_calculo_hibrido ON escala_dosis_fija (usuario_id, nombre_comida_personalizado, carbohidratos_gr);
