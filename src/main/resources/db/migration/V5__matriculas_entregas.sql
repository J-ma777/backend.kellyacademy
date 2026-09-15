-- =========================================
-- V5 - Matriculas y Entregas
-- =========================================
-- Tablas: matriculas, entregas
-- Matricula: estudiante inscrito en curso.
-- Entrega: estudiante entrega una tarea.
-- =========================================

-- -----------------------------------------
-- Tabla: matriculas
-- -----------------------------------------
CREATE TABLE matriculas (
                            id                  UUID PRIMARY KEY,
                            curso_id            UUID NOT NULL,
                            estudiante_id       UUID NOT NULL,
                            estado              VARCHAR(50) NOT NULL,
                            nota_final          NUMERIC(4, 2),
                            asistencia_porcentaje NUMERIC(5, 2),
                            matriculado_at      TIMESTAMP(6) NOT NULL,
                            fecha_creacion      TIMESTAMP(6) NOT NULL,
                            fecha_actualizacion TIMESTAMP(6) NOT NULL,
                            CONSTRAINT fk_matriculas_curso
                                FOREIGN KEY (curso_id) REFERENCES cursos (id) ON DELETE CASCADE,
                            CONSTRAINT fk_matriculas_estudiante
                                FOREIGN KEY (estudiante_id) REFERENCES usuarios (id),
                            CONSTRAINT uk_matriculas_curso_estudiante
                                UNIQUE (curso_id, estudiante_id),
                            CONSTRAINT chk_matriculas_estado
                                CHECK (estado IN ('ACTIVA', 'COMPLETADA', 'RIESGO', 'ABANDONADA')),
                            CONSTRAINT chk_matriculas_nota
                                CHECK (nota_final IS NULL OR (nota_final >= 0 AND nota_final <= 100)),
                            CONSTRAINT chk_matriculas_asistencia
                                CHECK (asistencia_porcentaje IS NULL OR (asistencia_porcentaje >= 0 AND asistencia_porcentaje <= 100))
);

CREATE INDEX idx_matriculas_curso ON matriculas (curso_id);
CREATE INDEX idx_matriculas_estudiante ON matriculas (estudiante_id);
CREATE INDEX idx_matriculas_estado ON matriculas (estado);

-- -----------------------------------------
-- Tabla: entregas
-- -----------------------------------------
CREATE TABLE entregas (
                          id                  UUID PRIMARY KEY,
                          tarea_id            UUID NOT NULL,
                          estudiante_id       UUID NOT NULL,
                          url_archivo         VARCHAR(500),
                          enviado_at          TIMESTAMP(6),
                          nota                NUMERIC(5, 2),
                          retroalimentacion   TEXT,
                          estado              VARCHAR(50) NOT NULL,
                          fecha_creacion      TIMESTAMP(6) NOT NULL,
                          fecha_actualizacion TIMESTAMP(6) NOT NULL,
                          CONSTRAINT fk_entregas_tarea
                              FOREIGN KEY (tarea_id) REFERENCES tareas (id) ON DELETE CASCADE,
                          CONSTRAINT fk_entregas_estudiante
                              FOREIGN KEY (estudiante_id) REFERENCES usuarios (id),
                          CONSTRAINT uk_entregas_tarea_estudiante
                              UNIQUE (tarea_id, estudiante_id),
                          CONSTRAINT chk_entregas_estado
                              CHECK (estado IN ('PENDIENTE', 'CALIFICADA', 'TARDE'))
);

CREATE INDEX idx_entregas_tarea ON entregas (tarea_id);
CREATE INDEX idx_entregas_estudiante ON entregas (estudiante_id);
CREATE INDEX idx_entregas_estado ON entregas (estado);