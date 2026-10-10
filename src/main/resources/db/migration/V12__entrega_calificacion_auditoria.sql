-- =========================================
-- V12 - Auditoria de calificacion de entregas
-- =========================================
-- Agrega calificado_at y calificado_por_id a entregas.
-- Registra quien califico y cuando, para auditoria.
-- Ambos nullable: las entregas no calificadas no tienen esos datos.
-- ON DELETE SET NULL: si se borra el usuario calificador, la entrega
-- conserva calificado_at pero pierde la referencia al usuario.
-- Consistente con fk_conversaciones_curso (ON DELETE SET NULL).
-- =========================================

ALTER TABLE entregas
    ADD COLUMN calificado_at TIMESTAMP(6);

ALTER TABLE entregas
    ADD COLUMN calificado_por_id UUID;

ALTER TABLE entregas
    ADD CONSTRAINT fk_entregas_calificado_por
        FOREIGN KEY (calificado_por_id) REFERENCES usuarios (id)
            ON DELETE SET NULL;

CREATE INDEX idx_entregas_calificado_por ON entregas (calificado_por_id);