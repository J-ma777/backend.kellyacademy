-- =========================================
-- V1 - Schema inicial
-- =========================================
-- Tablas: usuarios, roles, permisos, usuario_rol, rol_permiso
-- Creado a partir del modelo actual de Hibernate
-- =========================================

-- -----------------------------------------
-- Tabla: permisos
-- -----------------------------------------
CREATE TABLE permisos (
                          id                  UUID PRIMARY KEY,
                          nombre              VARCHAR(100) NOT NULL UNIQUE,
                          descripcion         VARCHAR(500),
                          fecha_creacion      TIMESTAMP(6) NOT NULL,
                          fecha_actualizacion TIMESTAMP(6) NOT NULL
);

-- -----------------------------------------
-- Tabla: roles
-- -----------------------------------------
CREATE TABLE roles (
                       id                  UUID PRIMARY KEY,
                       nombre              VARCHAR(100) NOT NULL UNIQUE,
                       descripcion         VARCHAR(500),
                       fecha_creacion      TIMESTAMP(6) NOT NULL,
                       fecha_actualizacion TIMESTAMP(6) NOT NULL
);

-- -----------------------------------------
-- Tabla: usuarios
-- -----------------------------------------
CREATE TABLE usuarios (
                          id                  UUID PRIMARY KEY,
                          nombre              VARCHAR(100) NOT NULL,
                          apellido            VARCHAR(100) NOT NULL,
                          correo_electronico  VARCHAR(255) NOT NULL UNIQUE,
                          contrasena          VARCHAR(255) NOT NULL,
                          avatar_url          VARCHAR(500),
                          estado              VARCHAR(255) NOT NULL,
                          fecha_creacion      TIMESTAMP(6) NOT NULL,
                          fecha_actualizacion TIMESTAMP(6) NOT NULL,
                          CONSTRAINT chk_usuarios_estado CHECK (estado IN ('ACTIVO', 'INACTIVO', 'BLOQUEADO'))
);

-- -----------------------------------------
-- Tabla intermedia: usuario_rol
-- -----------------------------------------
CREATE TABLE usuario_rol (
                             usuario_id UUID NOT NULL,
                             rol_id     UUID NOT NULL,
                             PRIMARY KEY (usuario_id, rol_id),
                             CONSTRAINT fk_usuario_rol_usuario
                                 FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
                             CONSTRAINT fk_usuario_rol_rol
                                 FOREIGN KEY (rol_id) REFERENCES roles (id)
);

-- -----------------------------------------
-- Tabla intermedia: rol_permiso
-- -----------------------------------------
CREATE TABLE rol_permiso (
                             rol_id     UUID NOT NULL,
                             permiso_id UUID NOT NULL,
                             PRIMARY KEY (rol_id, permiso_id),
                             CONSTRAINT fk_rol_permiso_rol
                                 FOREIGN KEY (rol_id) REFERENCES roles (id),
                             CONSTRAINT fk_rol_permiso_permiso
                                 FOREIGN KEY (permiso_id) REFERENCES permisos (id)
);

-- -----------------------------------------
-- Indices
-- -----------------------------------------
CREATE INDEX idx_usuarios_correo ON usuarios (correo_electronico);
CREATE INDEX idx_roles_nombre ON roles (nombre);
CREATE INDEX idx_permisos_nombre ON permisos (nombre);