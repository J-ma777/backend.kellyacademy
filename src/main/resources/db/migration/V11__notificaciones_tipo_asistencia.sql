-- =========================================
-- V11 - Habilitar tipo ASISTENCIA en notificaciones
-- =========================================
-- La migracion V7 definio chk_notificaciones_tipo con los 5 valores
-- originales del enum TipoNotificacion. El slice #62 agrego el valor
-- ASISTENCIA al enum Java (notificar al estudiante cuando la asistencia
-- es AUSENTE / TARDE / JUSTIFICADO) pero no agrego una migracion que
-- actualizara el CHECK. Los tests H2 no lo detectaron porque Flyway
-- esta deshabilitado en test y Hibernate no genera CHECK sobre enums.
--
-- Esta migracion realinea la DB de produccion con el enum Java.
-- Sin este fix, cualquier INSERT de una notificacion con tipo
-- ASISTENCIA viola chk_notificaciones_tipo en Postgres y devuelve 500.
-- =========================================

ALTER TABLE notificaciones
DROP CONSTRAINT chk_notificaciones_tipo;

ALTER TABLE notificaciones
    ADD CONSTRAINT chk_notificaciones_tipo
        CHECK (tipo IN (
                        'TAREA_NUEVA',
                        'CALIFICACION',
                        'MENSAJE',
                        'ANUNCIO',
                        'SISTEMA',
                        'ASISTENCIA'
            ));