CREATE TABLE archivos_biblioteca (
    id UUID PRIMARY KEY,
    usuario_id UUID REFERENCES usuarios(id) ON DELETE SET NULL,
    proveedor VARCHAR(20) NOT NULL,
    nombre VARCHAR(255) NOT NULL,
    mime VARCHAR(120) NOT NULL,
    bytes BIGINT NOT NULL CHECK (bytes > 0 AND bytes <= 20971520),
    public_id VARCHAR(255) NOT NULL UNIQUE,
    url VARCHAR(500) NOT NULL UNIQUE,
    fecha_creacion TIMESTAMP NOT NULL,
    fecha_actualizacion TIMESTAMP NOT NULL
);
