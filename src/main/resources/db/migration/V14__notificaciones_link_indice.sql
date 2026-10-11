-- =========================================
-- V14 - Indice sobre notificaciones.link
-- =========================================
-- El slice #87 agrega deduplicacion de notificaciones de asistencia:
-- cuando el docente corrige varias veces la misma asistencia y el estado
-- final ya fue notificado previamente, no se crea una notificacion nueva.
--
-- La deduplicacion se apoya en (link, cuerpo):
--   - link   = /api/asistencias/{id}  (identifica la entidad origen)
--   - cuerpo = texto ya notificado     (identifica el estado + contexto)
--
-- Sin indice sobre link, la query existsByLinkAndCuerpo hace full scan
-- sobre notificaciones. Con el volumen esperado del LMS (cientos de miles
-- de filas) y la frecuencia de correcciones de asistencia, el indice es
-- necesario. cuerpo queda como filtro secundario sobre pocas filas por link.
-- =========================================

CREATE INDEX idx_notificaciones_link ON notificaciones (link);