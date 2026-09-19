package com.kellyacademy.calendar.dto.response;

import com.kellyacademy.calendar.enums.EstadoTutoria;

import java.time.LocalDateTime;
import java.util.UUID;

public record TutoriaResumenResponse(
        UUID id,
        UUID estudianteId,
        String estudianteNombreCompleto,
        UUID docenteId,
        String docenteNombreCompleto,
        UUID cursoId,
        String cursoTitulo,
        LocalDateTime fecha,
        Integer duracionMinutos,
        EstadoTutoria estado
) {
}