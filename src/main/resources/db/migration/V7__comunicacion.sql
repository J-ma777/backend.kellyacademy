-- =========================================
-- V7 - Comunicacion
-- =========================================
-- Tablas: conversaciones, mensajes, anuncios, notificaciones
-- =========================================

-- -----------------------------------------
-- Tabla: conversaciones (hilo entre 2 usuarios)
-- -----------------------------------------
CREATE TABLE conversaciones (
                                id                  UUID PRIMARY KEY,
                                curso_id            UUID,
                                participante_1_id   UUID NOT NULL,
                                participante_2_id   UUID NOT NULL,
                                asunto              VARCHAR(200),
                                ultimo_mensaje_at   TIMESTAMP(6),
                                fecha_creacion      TIMESTAMP(6) NOT NULL,
                                fecha_actualizacion TIMESTAMP(6) NOT NULL,
                                CONSTRAINT fk_conversaciones_curso
                                    FOREIGN KEY (curso_id) REFERENCES cursos (id) ON DELETE SET NULL,
                                CONSTRAINT fk_conversaciones_participante_1
                                    FOREIGN KEY (participante_1_id) REFERENCES usuarios (id),
                                CONSTRAINT fk_conversaciones_participante_2
                                    FOREIGN KEY (participante_2_id) REFERENCES usuarios (id),
                                CONSTRAINT chk_conversaciones_participantes_distintos
                                    CHECK (participante_1_id <> participante_2_id),
                                CONSTRAINT uk_conversaciones_participantes_curso
                                    UNIQUE (curso_id, participante_1_id, participante_2_id)
);

CREATE INDEX idx_conversaciones_participante_1 ON conversaciones (participante_1_id);
CREATE INDEX idx_conversaciones_participante_2 ON conversaciones (participante_2_id);
CREATE INDEX idx_conversaciones_ultimo_mensaje ON conversaciones (ultimo_mensaje_at DESC);

-- -----------------------------------------
-- Tabla: mensajes (dentro de una conversacion)
-- -----------------------------------------
CREATE TABLE mensajes (
                          id                  UUID PRIMARY KEY,
                          conversacion_id     UUID NOT NULL,
                          remitente_id        UUID NOT NULL,
                          cuerpo              TEXT NOT NULL,
                          adjunto_url         VARCHAR(500),
                          leido               BOOLEAN NOT NULL DEFAULT FALSE,
                          enviado_at          TIMESTAMP(6) NOT NULL,
                          fecha_creacion      TIMESTAMP(6) NOT NULL,
                          fecha_actualizacion TIMESTAMP(6) NOT NULL,
                          CONSTRAINT fk_mensajes_conversacion
                              FOREIGN KEY (conversacion_id) REFERENCES conversaciones (id) ON DELETE CASCADE,
                          CONSTRAINT fk_mensajes_remitente
                              FOREIGN KEY (remitente_id) REFERENCES usuarios (id)
);

CREATE INDEX idx_mensajes_conversacion ON mensajes (conversacion_id);
CREATE INDEX idx_mensajes_enviado_at ON mensajes (enviado_at);

-- -----------------------------------------
-- Tabla: anuncios (comunicado del docente al curso)
-- -----------------------------------------
CREATE TABLE anuncios (
                          id                  UUID PRIMARY KEY,
                          curso_id            UUID NOT NULL,
                          autor_id            UUID NOT NULL,
                          titulo              VARCHAR(200) NOT NULL,
                          cuerpo              TEXT NOT NULL,
                          activo              BOOLEAN NOT NULL DEFAULT TRUE,
                          fecha_creacion      TIMESTAMP(6) NOT NULL,
                          fecha_actualizacion TIMESTAMP(6) NOT NULL,
                          CONSTRAINT fk_anuncios_curso
                              FOREIGN KEY (curso_id) REFERENCES cursos (id) ON DELETE CASCADE,
                          CONSTRAINT fk_anuncios_autor
                              FOREIGN KEY (autor_id) REFERENCES usuarios (id)
);

CREATE INDEX idx_anuncios_curso ON anuncios (curso_id);
CREATE INDEX idx_anuncios_activo ON anuncios (activo);
CREATE INDEX idx_anuncios_creacion ON anuncios (fecha_creacion DESC);

-- -----------------------------------------
-- Tabla: notificaciones (sistema -> usuario)
-- -----------------------------------------
CREATE TABLE notificaciones (
                                id                  UUID PRIMARY KEY,
                                usuario_id          UUID NOT NULL,
                                tipo                VARCHAR(50) NOT NULL,
                                titulo              VARCHAR(200) NOT NULL,
                                cuerpo              VARCHAR(500),
                                link                VARCHAR(500),
                                leida               BOOLEAN NOT NULL DEFAULT FALSE,
                                fecha_creacion      TIMESTAMP(6) NOT NULL,
                                fecha_actualizacion TIMESTAMP(6) NOT NULL,
                                CONSTRAINT fk_notificaciones_usuario
                                    FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE,
                                CONSTRAINT chk_notificaciones_tipo
                                    CHECK (tipo IN ('TAREA_NUEVA', 'CALIFICACION', 'MENSAJE', 'ANUNCIO', 'SISTEMA'))
);

CREATE INDEX idx_notificaciones_usuario ON notificaciones (usuario_id);
CREATE INDEX idx_notificaciones_leida ON notificaciones (leida);
CREATE INDEX idx_notificaciones_creacion ON notificaciones (fecha_creacion DESC);