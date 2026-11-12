CREATE TABLE aceptaciones_terminos (
    id UUID PRIMARY KEY,
    usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    version VARCHAR(40) NOT NULL,
    roles VARCHAR(500) NOT NULL,
    aceptado_en TIMESTAMP NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL,
    fecha_actualizacion TIMESTAMP NOT NULL
);
CREATE INDEX idx_aceptaciones_terminos_usuario_fecha
    ON aceptaciones_terminos(usuario_id, aceptado_en DESC);
