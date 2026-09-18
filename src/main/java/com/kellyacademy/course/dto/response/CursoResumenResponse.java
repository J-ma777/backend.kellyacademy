package com.kellyacademy.course.dto.response;

import com.kellyacademy.course.enums.EstadoCurso;
import com.kellyacademy.course.enums.NivelCefr;

import java.util.UUID;

// Para listados y selects. Sin descripcion ni fechas de auditoria.
public record CursoResumenResponse(
        UUID id,
        String titulo,
        NivelCefr nivelCefr,
        String horario,
        EstadoCurso estado,
        UUID docenteId
) {
}