package com.kellyacademy.course.dto.response;

import com.kellyacademy.course.enums.EstadoCurso;
import com.kellyacademy.course.enums.NivelCefr;

import java.util.UUID;

// Datos de la tarjeta de curso. Sin colecciones ni fechas de auditoria.
public record CursoResumenResponse(
        UUID id,
        String titulo,
        NivelCefr nivelCefr,
        String horario,
        EstadoCurso estado,
        UUID docenteId,
        String descripcion,
        String docenteNombreCompleto
) {
}
