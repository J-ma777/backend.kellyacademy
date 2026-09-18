package com.kellyacademy.course.dto.response;

import com.kellyacademy.course.enums.EstadoCurso;
import com.kellyacademy.course.enums.NivelCefr;
import com.kellyacademy.user.dto.response.UsuarioResumenResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record CursoResponse(
        UUID id,
        UsuarioResumenResponse docente,
        String titulo,
        String descripcion,
        NivelCefr nivelCefr,
        String horario,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        Integer capacidadMaxima,
        EstadoCurso estado,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion
) {
}