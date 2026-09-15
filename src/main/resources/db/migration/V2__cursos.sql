-- =========================================
-- V2 - Modulo de Cursos
-- =========================================
-- Tabla: cursos
-- Almacena los cursos del LMS con su docente,
-- nivel CEFR, horario y estado.
-- =========================================

CREATE TABLE cursos (
                        id                  UUID PRIMARY KEY,
                        docente_id          UUID NOT NULL,
                        titulo              VARCHAR(200) NOT NULL,
                        descripcion         TEXT,
                        nivel_cefr          VARCHAR(10) NOT NULL,
                        horario             VARCHAR(200),
                        fecha_inicio        DATE,
                        fecha_fin           DATE,
                        capacidad_maxima    INTEGER NOT NULL DEFAULT 30,
                        estado              VARCHAR(50) NOT NULL,
                        fecha_creacion      TIMESTAMP(6) NOT NULL,
                        fecha_actualizacion TIMESTAMP(6) NOT NULL,
                        CONSTRAINT fk_cursos_docente
                            FOREIGN KEY (docente_id) REFERENCES usuarios (id),
                        CONSTRAINT chk_cursos_nivel_cefr
                            CHECK (nivel_cefr IN ('A1', 'A2', 'B1', 'B2', 'C1', 'C2')),
                        CONSTRAINT chk_cursos_estado
                            CHECK (estado IN ('BORRADOR', 'ACTIVO', 'FINALIZADO', 'ARCHIVADO')),
                        CONSTRAINT chk_cursos_capacidad
                            CHECK (capacidad_maxima > 0)
);

CREATE INDEX idx_cursos_docente ON cursos (docente_id);
CREATE INDEX idx_cursos_estado ON cursos (estado);
CREATE INDEX idx_cursos_nivel_cefr ON cursos (nivel_cefr);