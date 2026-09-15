-- =========================================
-- V4 - Contenido de semanas
-- =========================================
-- Tablas: clases, materiales, tareas
-- Las tres pertenecen a una semana.
-- Modeladas como tablas separadas (sin herencia JPA).
-- =========================================

-- -----------------------------------------
-- Tabla: clases (sesiones en vivo)
-- -----------------------------------------
CREATE TABLE clases (
                        id                  UUID PRIMARY KEY,
                        semana_id           UUID NOT NULL,
                        titulo              VARCHAR(200) NOT NULL,
                        descripcion         TEXT,
                        url_vivo            VARCHAR(500),
                        url_grabacion       VARCHAR(500),
                        fecha_hora          TIMESTAMP(6),
                        duracion_minutos    INTEGER,
                        sala                VARCHAR(100),
                        fecha_creacion      TIMESTAMP(6) NOT NULL,
                        fecha_actualizacion TIMESTAMP(6) NOT NULL,
                        CONSTRAINT fk_clases_semana
                            FOREIGN KEY (semana_id) REFERENCES semanas (id) ON DELETE CASCADE,
                        CONSTRAINT chk_clases_duracion
                            CHECK (duracion_minutos IS NULL OR duracion_minutos > 0)
);

CREATE INDEX idx_clases_semana ON clases (semana_id);
CREATE INDEX idx_clases_fecha ON clases (fecha_hora);

-- -----------------------------------------
-- Tabla: materiales (recursos de la semana)
-- -----------------------------------------
CREATE TABLE materiales (
                            id                  UUID PRIMARY KEY,
                            semana_id           UUID NOT NULL,
                            titulo              VARCHAR(200) NOT NULL,
                            descripcion         TEXT,
                            url_archivo         VARCHAR(500),
                            tipo                VARCHAR(50),
                            tamano_mb           NUMERIC(10, 2),
                            url_externo         VARCHAR(500),
                            fecha_creacion      TIMESTAMP(6) NOT NULL,
                            fecha_actualizacion TIMESTAMP(6) NOT NULL,
                            CONSTRAINT fk_materiales_semana
                                FOREIGN KEY (semana_id) REFERENCES semanas (id) ON DELETE CASCADE,
                            CONSTRAINT chk_materiales_tipo
                                CHECK (tipo IS NULL OR tipo IN ('PDF', 'AUDIO', 'VIDEO', 'ENLACE', 'DOCUMENTO', 'IMAGEN'))
);

CREATE INDEX idx_materiales_semana ON materiales (semana_id);
CREATE INDEX idx_materiales_tipo ON materiales (tipo);

-- -----------------------------------------
-- Tabla: tareas (asignaciones de la semana)
-- -----------------------------------------
CREATE TABLE tareas (
                        id                  UUID PRIMARY KEY,
                        semana_id           UUID NOT NULL,
                        titulo              VARCHAR(200) NOT NULL,
                        descripcion         TEXT,
                        instrucciones_url   VARCHAR(500),
                        fecha_limite        TIMESTAMP(6),
                        puntaje_maximo      INTEGER NOT NULL DEFAULT 100,
                        fecha_creacion      TIMESTAMP(6) NOT NULL,
                        fecha_actualizacion TIMESTAMP(6) NOT NULL,
                        CONSTRAINT fk_tareas_semana
                            FOREIGN KEY (semana_id) REFERENCES semanas (id) ON DELETE CASCADE,
                        CONSTRAINT chk_tareas_puntaje
                            CHECK (puntaje_maximo > 0)
);

CREATE INDEX idx_tareas_semana ON tareas (semana_id);
CREATE INDEX idx_tareas_fecha_limite ON tareas (fecha_limite);