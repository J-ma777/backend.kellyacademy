-- =========================================
-- V8 - Calendario y Tutorias
-- =========================================
-- Tablas: eventos, disponibilidad_tutoria, tutorias
-- =========================================

-- -----------------------------------------
-- Tabla: eventos (eventos manuales del calendario)
-- -----------------------------------------
CREATE TABLE eventos (
                         id                  UUID PRIMARY KEY,
                         usuario_id          UUID NOT NULL,
                         curso_id            UUID,
                         titulo              VARCHAR(200) NOT NULL,
                         descripcion         TEXT,
                         tipo                VARCHAR(50) NOT NULL,
                         inicio              TIMESTAMP(6) NOT NULL,
                         fin                 TIMESTAMP(6),
                         sala                VARCHAR(100),
                         fecha_creacion      TIMESTAMP(6) NOT NULL,
                         fecha_actualizacion TIMESTAMP(6) NOT NULL,
                         CONSTRAINT fk_eventos_usuario
                             FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE,
                         CONSTRAINT fk_eventos_curso
                             FOREIGN KEY (curso_id) REFERENCES cursos (id) ON DELETE SET NULL,
                         CONSTRAINT chk_eventos_tipo
                             CHECK (tipo IN ('CLASE', 'TAREA', 'EXAMEN', 'TUTORIA', 'REUNION', 'OTRO')),
                         CONSTRAINT chk_eventos_fechas
                             CHECK (fin IS NULL OR fin >= inicio)
);

CREATE INDEX idx_eventos_usuario ON eventos (usuario_id);
CREATE INDEX idx_eventos_curso ON eventos (curso_id);
CREATE INDEX idx_eventos_inicio ON eventos (inicio);

-- -----------------------------------------
-- Tabla: disponibilidad_tutoria (horarios del docente)
-- -----------------------------------------
CREATE TABLE disponibilidad_tutoria (
                                        id                  UUID PRIMARY KEY,
                                        docente_id          UUID NOT NULL,
                                        dia_semana          VARCHAR(20) NOT NULL,
                                        hora_inicio         TIME NOT NULL,
                                        hora_fin            TIME NOT NULL,
                                        bloqueada           BOOLEAN NOT NULL DEFAULT FALSE,
                                        fecha_creacion      TIMESTAMP(6) NOT NULL,
                                        fecha_actualizacion TIMESTAMP(6) NOT NULL,
                                        CONSTRAINT fk_disponibilidad_docente
                                            FOREIGN KEY (docente_id) REFERENCES usuarios (id) ON DELETE CASCADE,
                                        CONSTRAINT chk_disponibilidad_dia
                                            CHECK (dia_semana IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY')),
                                        CONSTRAINT chk_disponibilidad_horas
                                            CHECK (hora_fin > hora_inicio)
);

CREATE INDEX idx_disponibilidad_docente ON disponibilidad_tutoria (docente_id);
CREATE INDEX idx_disponibilidad_dia ON disponibilidad_tutoria (dia_semana);

-- -----------------------------------------
-- Tabla: tutorias (sesiones 1:1 reservadas)
-- -----------------------------------------
CREATE TABLE tutorias (
                          id                  UUID PRIMARY KEY,
                          estudiante_id       UUID NOT NULL,
                          docente_id          UUID NOT NULL,
                          curso_id            UUID,
                          fecha               TIMESTAMP(6) NOT NULL,
                          duracion_minutos    INTEGER NOT NULL,
                          estado              VARCHAR(50) NOT NULL,
                          notas               TEXT,
                          fecha_creacion      TIMESTAMP(6) NOT NULL,
                          fecha_actualizacion TIMESTAMP(6) NOT NULL,
                          CONSTRAINT fk_tutorias_estudiante
                              FOREIGN KEY (estudiante_id) REFERENCES usuarios (id),
                          CONSTRAINT fk_tutorias_docente
                              FOREIGN KEY (docente_id) REFERENCES usuarios (id),
                          CONSTRAINT fk_tutorias_curso
                              FOREIGN KEY (curso_id) REFERENCES cursos (id) ON DELETE SET NULL,
                          CONSTRAINT chk_tutorias_estado
                              CHECK (estado IN ('PENDIENTE', 'CONFIRMADA', 'CANCELADA', 'COMPLETADA')),
                          CONSTRAINT chk_tutorias_duracion
                              CHECK (duracion_minutos > 0),
                          CONSTRAINT chk_tutorias_participantes_distintos
                              CHECK (estudiante_id <> docente_id)
);

CREATE INDEX idx_tutorias_estudiante ON tutorias (estudiante_id);
CREATE INDEX idx_tutorias_docente ON tutorias (docente_id);
CREATE INDEX idx_tutorias_fecha ON tutorias (fecha);
CREATE INDEX idx_tutorias_estado ON tutorias (estado);