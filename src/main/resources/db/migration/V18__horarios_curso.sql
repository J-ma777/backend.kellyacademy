ALTER TABLE cursos ALTER COLUMN horario TYPE VARCHAR(1000);
CREATE TABLE horarios_curso (
    id UUID PRIMARY KEY,
    curso_id UUID NOT NULL REFERENCES cursos(id) ON DELETE CASCADE,
    dia VARCHAR(10) NOT NULL CHECK (dia IN ('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY')),
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    fecha_creacion TIMESTAMP(6) NOT NULL,
    fecha_actualizacion TIMESTAMP(6) NOT NULL,
    CONSTRAINT chk_horario_rango CHECK (hora_fin > hora_inicio)
);
CREATE INDEX idx_horarios_curso ON horarios_curso(curso_id);
