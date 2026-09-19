package com.kellyacademy.calendar.dto.response;

import com.kellyacademy.calendar.enums.EstadoTutoria;

import java.time.LocalDateTime;
import java.util.UUID;

public record TutoriaResponse(
        UUID id,
        UUID estudianteId,
        String estudianteNombreCompleto,
        String estudianteCorreo,
        UUID docenteId,
        String docenteNombreCompleto,
        UUID cursoId,
        String cursoTitulo,
        LocalDateTime fecha,
        Integer duracionMinutos,
        EstadoTutoria estado,
        String notas,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion
) {
}