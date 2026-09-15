-- =========================================
-- V3 - Estructura academica
-- =========================================
-- Tablas: unidades, semanas
-- Una unidad agrupa varias semanas.
-- Una semana pertenece a una unidad.
-- =========================================

-- -----------------------------------------
-- Tabla: unidades
-- -----------------------------------------
CREATE TABLE unidades (
                          id                  UUID PRIMARY KEY,
                          curso_id            UUID NOT NULL,
                          numero              INTEGER NOT NULL,
                          titulo              VARCHAR(200) NOT NULL,
                          descripcion         TEXT,
                          fecha_creacion      TIMESTAMP(6) NOT NULL,
                          fecha_actualizacion TIMESTAMP(6) NOT NULL,
                          CONSTRAINT fk_unidades_curso
                              FOREIGN KEY (curso_id) REFERENCES cursos (id) ON DELETE CASCADE,
                          CONSTRAINT chk_unidades_numero
                              CHECK (numero > 0),
                          CONSTRAINT uk_unidades_curso_numero
                              UNIQUE (curso_id, numero)
);

CREATE INDEX idx_unidades_curso ON unidades (curso_id);

-- -----------------------------------------
-- Tabla: semanas
-- -----------------------------------------
CREATE TABLE semanas (
                         id                  UUID PRIMARY KEY,
                         unidad_id           UUID NOT NULL,
                         numero              INTEGER NOT NULL,
                         titulo              VARCHAR(200) NOT NULL,
                         descripcion         TEXT,
                         es_actual           BOOLEAN NOT NULL DEFAULT FALSE,
                         fecha_creacion      TIMESTAMP(6) NOT NULL,
                         fecha_actualizacion TIMESTAMP(6) NOT NULL,
                         CONSTRAINT fk_semanas_unidad
                             FOREIGN KEY (unidad_id) REFERENCES unidades (id) ON DELETE CASCADE,
                         CONSTRAINT chk_semanas_numero
                             CHECK (numero > 0),
                         CONSTRAINT uk_semanas_unidad_numero
                             UNIQUE (unidad_id, numero)
);

CREATE INDEX idx_semanas_unidad ON semanas (unidad_id);
CREATE INDEX idx_semanas_es_actual ON semanas (es_actual);