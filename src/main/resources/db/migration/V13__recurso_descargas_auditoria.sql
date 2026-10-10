-- =========================================
-- V13 - Auditoria de descargas de recursos
-- =========================================
-- Tabla: recurso_descargas
-- Registra cada descarga individual de un recurso de biblioteca:
-- quien (usuario_id), desde donde (ip) y cuando (fecha_descarga).
--
-- Cardinalidad 1:N respecto a recursos_biblioteca (un recurso puede
-- descargarse muchas veces) y 1:N respecto a usuarios. Por eso tabla
-- separada y no columnas adicionales en recursos_biblioteca.
--
-- FKs:
--   - recurso_id -> recursos_biblioteca(id) ON DELETE CASCADE:
--     si se borra el recurso, sus trazas de descarga no tienen sentido.
--   - usuario_id -> usuarios(id) ON DELETE SET NULL:
--     consistente con fk_entregas_calificado_por (V12). La traza
--     sobrevive al borrado del usuario; se pierde el "quien" pero
--     se conserva el "cuando" y el "que recurso". usuario_id nullable.
--
-- ip VARCHAR(45): soporta IPv4 (15) e IPv6 (45).
-- =========================================

CREATE TABLE recurso_descargas (
                                   id              UUID PRIMARY KEY,
                                   recurso_id      UUID NOT NULL,
                                   usuario_id      UUID,
                                   ip              VARCHAR(45) NOT NULL,
                                   fecha_descarga  TIMESTAMP(6) NOT NULL,
                                   fecha_creacion      TIMESTAMP(6) NOT NULL,
                                   fecha_actualizacion TIMESTAMP(6) NOT NULL,
                                   CONSTRAINT fk_recurso_descargas_recurso
                                       FOREIGN KEY (recurso_id) REFERENCES recursos_biblioteca (id)
                                           ON DELETE CASCADE,
                                   CONSTRAINT fk_recurso_descargas_usuario
                                       FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
                                           ON DELETE SET NULL
);

CREATE INDEX idx_recurso_descargas_recurso ON recurso_descargas (recurso_id);
CREATE INDEX idx_recurso_descargas_usuario ON recurso_descargas (usuario_id);
CREATE INDEX idx_recurso_descargas_fecha   ON recurso_descargas (fecha_descarga);