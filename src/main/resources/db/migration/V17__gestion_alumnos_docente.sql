ALTER TABLE usuarios ADD COLUMN telefono VARCHAR(9);
ALTER TABLE usuarios ADD CONSTRAINT chk_usuarios_telefono CHECK (telefono IS NULL OR telefono ~ '^[0-9]{9}$');

ALTER TABLE usuario_rol DROP CONSTRAINT fk_usuario_rol_usuario;
ALTER TABLE usuario_rol ADD CONSTRAINT fk_usuario_rol_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE;
ALTER TABLE matriculas DROP CONSTRAINT fk_matriculas_estudiante;
ALTER TABLE matriculas ADD CONSTRAINT fk_matriculas_estudiante FOREIGN KEY (estudiante_id) REFERENCES usuarios(id) ON DELETE CASCADE;
ALTER TABLE entregas DROP CONSTRAINT fk_entregas_estudiante;
ALTER TABLE entregas ADD CONSTRAINT fk_entregas_estudiante FOREIGN KEY (estudiante_id) REFERENCES usuarios(id) ON DELETE CASCADE;
ALTER TABLE asistencias DROP CONSTRAINT fk_asistencias_estudiante;
ALTER TABLE asistencias ADD CONSTRAINT fk_asistencias_estudiante FOREIGN KEY (estudiante_id) REFERENCES usuarios(id) ON DELETE CASCADE;
ALTER TABLE tutorias DROP CONSTRAINT fk_tutorias_estudiante;
ALTER TABLE tutorias ADD CONSTRAINT fk_tutorias_estudiante FOREIGN KEY (estudiante_id) REFERENCES usuarios(id) ON DELETE CASCADE;
ALTER TABLE conversaciones DROP CONSTRAINT fk_conversaciones_participante_1;
ALTER TABLE conversaciones ADD CONSTRAINT fk_conversaciones_participante_1 FOREIGN KEY (participante_1_id) REFERENCES usuarios(id) ON DELETE CASCADE;
ALTER TABLE conversaciones DROP CONSTRAINT fk_conversaciones_participante_2;
ALTER TABLE conversaciones ADD CONSTRAINT fk_conversaciones_participante_2 FOREIGN KEY (participante_2_id) REFERENCES usuarios(id) ON DELETE CASCADE;
ALTER TABLE mensajes DROP CONSTRAINT fk_mensajes_remitente;
ALTER TABLE mensajes ADD CONSTRAINT fk_mensajes_remitente FOREIGN KEY (remitente_id) REFERENCES usuarios(id) ON DELETE CASCADE;
