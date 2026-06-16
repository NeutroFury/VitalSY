CREATE TABLE escala_dosis_fija (
    id SERIAL PRIMARY KEY,
    usuario_id INT NOT NULL,
    momento_dia VARCHAR(50) NOT NULL,
    glicemia_min INT NOT NULL,
    glicemia_max INT NOT NULL,
    carbohidratos_gr FLOAT8 NOT NULL,
    dosis_insulina FLOAT8 NOT NULL,
    CONSTRAINT fk_escala_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    CONSTRAINT chk_momento_dia CHECK (momento_dia IN ('DESAYUNO', 'ALMUERZO', 'ONCE_CENA_SIN_EJERCICIO', 'ONCE_CENA_CON_EJERCICIO'))
);
CREATE INDEX idx_escala_calculo_hibrido ON escala_dosis_fija (usuario_id, momento_dia, carbohidratos_gr);
