-- =========================================
-- V9 - Biblioteca Virtual
-- =========================================
-- Tabla: recursos_biblioteca
-- Almacena material educativo compartido entre cursos.
-- =========================================

CREATE TABLE recursos_biblioteca (
                                     id                  UUID PRIMARY KEY,
                                     titulo              VARCHAR(200) NOT NULL,
                                     descripcion         TEXT,
                                     categoria           VARCHAR(100),
                                     nivel_cefr          VARCHAR(10),
                                     tipo                VARCHAR(50) NOT NULL,
                                     url_archivo         VARCHAR(500),
                                     url_externo         VARCHAR(500),
                                     tamano_mb           NUMERIC(10, 2),
                                     contador_descargas  INTEGER NOT NULL DEFAULT 0,
                                     fecha_creacion      TIMESTAMP(6) NOT NULL,
                                     fecha_actualizacion TIMESTAMP(6) NOT NULL,
                                     CONSTRAINT chk_recursos_nivel_cefr
                                         CHECK (nivel_cefr IS NULL OR nivel_cefr IN ('A1', 'A2', 'B1', 'B2', 'C1', 'C2')),
                                     CONSTRAINT chk_recursos_tipo
                                         CHECK (tipo IN ('PDF', 'AUDIO', 'VIDEO', 'ENLACE', 'DOCUMENTO', 'IMAGEN')),
                                     CONSTRAINT chk_recursos_contador
                                         CHECK (contador_descargas >= 0)
);

CREATE INDEX idx_recursos_categoria ON recursos_biblioteca (categoria);
CREATE INDEX idx_recursos_nivel_cefr ON recursos_biblioteca (nivel_cefr);
CREATE INDEX idx_recursos_tipo ON recursos_biblioteca (tipo);
CREATE INDEX idx_recursos_titulo ON recursos_biblioteca (titulo);