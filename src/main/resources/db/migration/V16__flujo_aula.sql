ALTER TABLE materiales ADD COLUMN publicado BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE materiales ADD COLUMN visible BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE clases ADD COLUMN publicado BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE clases ADD COLUMN visible BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE tareas ADD COLUMN publicado BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE tareas ADD COLUMN visible BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE tareas ADD COLUMN tipo_actividad VARCHAR(20) NOT NULL DEFAULT 'TAREA';
ALTER TABLE tareas ADD CONSTRAINT chk_tipo_actividad CHECK (tipo_actividad IN ('TAREA','EVALUACION','QUIZ'));
ALTER TABLE entregas ADD COLUMN texto_respuesta TEXT;
ALTER TABLE anuncios ADD COLUMN publicado BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE anuncios ADD COLUMN fijado BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE anuncios ADD COLUMN publicado_at TIMESTAMP(6);
UPDATE anuncios SET publicado_at = fecha_creacion;
CREATE TABLE progreso_contenido (
    id UUID PRIMARY KEY,
    estudiante_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    material_id UUID REFERENCES materiales(id) ON DELETE CASCADE,
    clase_id UUID REFERENCES clases(id) ON DELETE CASCADE,
    fecha_creacion TIMESTAMP(6) NOT NULL,
    fecha_actualizacion TIMESTAMP(6) NOT NULL,
    CONSTRAINT chk_progreso_recurso CHECK ((material_id IS NULL) <> (clase_id IS NULL)),
    CONSTRAINT uk_progreso_material UNIQUE (estudiante_id, material_id),
    CONSTRAINT uk_progreso_clase UNIQUE (estudiante_id, clase_id)
);
CREATE INDEX idx_progreso_material ON progreso_contenido(material_id);
CREATE INDEX idx_progreso_clase ON progreso_contenido(clase_id);
