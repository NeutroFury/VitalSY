-- Extensión para asegurar que los UUIDs o funciones de tiempo funcionen correctamente
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

---
-- 1. TABLA DE USUARIOS
---
CREATE TABLE usuarios (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    fecha_nacimiento DATE,
    genero VARCHAR(20),
    peso_actual DECIMAL(5,2),
    rol VARCHAR(20) DEFAULT 'PACIENTE',
    activo BOOLEAN DEFAULT TRUE,
    creado_en TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

---
-- 2. TABLA DE PARÁMETROS CLÍNICOS (CU03)
---
CREATE TABLE parametros_clinicos (
    id SERIAL PRIMARY KEY,
    usuario_id INT UNIQUE REFERENCES usuarios(id) ON DELETE CASCADE,
    ratio_carbohidratos DECIMAL(5,2) NOT NULL,
    factor_sensibilidad DECIMAL(5,2) NOT NULL,
    objetivo_glucemia_min INT DEFAULT 70,
    objetivo_glucemia_max INT DEFAULT 150,
    tiempo_accion_insulina INT DEFAULT 3,
    unidad_medida VARCHAR(10) DEFAULT 'mg/dL',
    ultima_actualizacion TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

---
-- 3. TABLA DE REGISTROS DE GLUCEMIA
---
CREATE TABLE registros_glucemia (
    id SERIAL PRIMARY KEY,
    usuario_id INT REFERENCES usuarios(id) ON DELETE CASCADE,
    valor_mgdl INT NOT NULL,
    tipo_registro VARCHAR(20) NOT NULL,
    tendencia VARCHAR(20),
    dispositivo_id VARCHAR(100),
    comentarios TEXT,
    analisis_ia TEXT,
    fecha_hora TIMESTAMP WITH TIME ZONE NOT NULL,
    creado_en TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

---
-- 4. TABLA DE REGISTROS DE NUTRICIÓN
---
CREATE TABLE registros_nutricion (
    id SERIAL PRIMARY KEY,
    usuario_id INT REFERENCES usuarios(id) ON DELETE CASCADE,
    descripcion_comida VARCHAR(255),
    carbohidratos_gr DECIMAL(6,2) NOT NULL,
    proteinas_gr DECIMAL(6,2),
    grasas_gr DECIMAL(6,2),
    calorias_kcal INT,
    momento_dia VARCHAR(20),
    estado_animo VARCHAR(50),
    foto_url VARCHAR(255),
    fecha_hora TIMESTAMP WITH TIME ZONE NOT NULL
);

---
-- 5. TABLA DE REGISTROS DE INSULINA
---
CREATE TABLE registros_insulina (
    id SERIAL PRIMARY KEY,
    usuario_id INT REFERENCES usuarios(id) ON DELETE CASCADE,
    unidades_sugeridas DECIMAL(5,2),
    unidades_aplicadas DECIMAL(5,2) NOT NULL,
    tipo_insulina VARCHAR(50),
    sitio_aplicacion VARCHAR(50),
    registro_glucemia_id INT REFERENCES registros_glucemia(id),
    registro_nutricion_id INT REFERENCES registros_nutricion(id),
    fecha_hora TIMESTAMP WITH TIME ZONE NOT NULL
);

---
-- 6. TABLA DE ALERTAS Y PREDICCIONES
---
CREATE TABLE alertas_predicciones (
    id SERIAL PRIMARY KEY,
    usuario_id INT REFERENCES usuarios(id) ON DELETE CASCADE,
    tipo_alerta VARCHAR(50),
    probabilidad DECIMAL(3,2),
    mensaje_notificacion TEXT,
    leida BOOLEAN DEFAULT FALSE,
    fecha_hora TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

---
-- ÍNDICES
---
CREATE INDEX idx_glucemia_fecha ON registros_glucemia (usuario_id, fecha_hora DESC);
CREATE INDEX idx_nutricion_fecha ON registros_nutricion (usuario_id, fecha_hora DESC);
CREATE INDEX idx_insulina_fecha ON registros_insulina (usuario_id, fecha_hora DESC);
CREATE INDEX idx_usuarios_email ON usuarios(email);
CREATE INDEX idx_alertas_usuario ON alertas_predicciones (usuario_id, leida);
