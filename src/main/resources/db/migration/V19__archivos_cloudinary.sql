CREATE TABLE archivos (
 id UUID PRIMARY KEY,
 curso_id UUID NOT NULL REFERENCES cursos(id) ON DELETE CASCADE,
 usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
 destino VARCHAR(20) NOT NULL CHECK (destino IN ('MATERIAL','ENTREGA')),
 nombre VARCHAR(255) NOT NULL,
 mime VARCHAR(120) NOT NULL,
 bytes BIGINT NOT NULL CHECK (bytes > 0 AND bytes <= 20971520),
 public_id VARCHAR(255) NOT NULL UNIQUE,
 resource_type VARCHAR(10) NOT NULL,
 url VARCHAR(500) NOT NULL,
 fecha_creacion TIMESTAMP NOT NULL,
 fecha_actualizacion TIMESTAMP NOT NULL
);
CREATE INDEX idx_archivos_curso ON archivos(curso_id);
CREATE INDEX idx_archivos_usuario ON archivos(usuario_id);
