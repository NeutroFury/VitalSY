CREATE TABLE librelinkup_config (
    id SERIAL PRIMARY KEY,
    usuario_id INT UNIQUE NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    libre_email VARCHAR(255) NOT NULL,
    libre_password VARCHAR(255) NOT NULL,
    libre_patient_id VARCHAR(100),
    activo BOOLEAN DEFAULT TRUE NOT NULL,
    ultimo_sync TIMESTAMP WITH TIME ZONE,
    creado_en TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    actualizado_en TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_librelinkup_config_usuario ON librelinkup_config (usuario_id);
