-- =========================================
-- V6 - Asistencias
-- =========================================
-- Tabla: asistencias
-- Registra la asistencia de cada estudiante a cada clase.
-- El porcentaje de asistencia se CALCULA desde estos registros.
-- =========================================

CREATE TABLE asistencias (
                             id                  UUID PRIMARY KEY,
                             clase_id            UUID NOT NULL,
                             estudiante_id       UUID NOT NULL,
                             estado              VARCHAR(50) NOT NULL,
                             observacion         VARCHAR(500),
                             registrado_at       TIMESTAMP(6) NOT NULL,
                             fecha_creacion      TIMESTAMP(6) NOT NULL,
                             fecha_actualizacion TIMESTAMP(6) NOT NULL,
                             CONSTRAINT fk_asistencias_clase
                                 FOREIGN KEY (clase_id) REFERENCES clases (id) ON DELETE CASCADE,
                             CONSTRAINT fk_asistencias_estudiante
                                 FOREIGN KEY (estudiante_id) REFERENCES usuarios (id),
                             CONSTRAINT uk_asistencias_clase_estudiante
                                 UNIQUE (clase_id, estudiante_id),
                             CONSTRAINT chk_asistencias_estado
                                 CHECK (estado IN ('PRESENTE', 'AUSENTE', 'TARDE', 'JUSTIFICADO'))
);

CREATE INDEX idx_asistencias_clase ON asistencias (clase_id);
CREATE INDEX idx_asistencias_estudiante ON asistencias (estudiante_id);
CREATE INDEX idx_asistencias_estado ON asistencias (estado);