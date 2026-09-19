package com.kellyacademy.attendance.dto.response;

import com.kellyacademy.attendance.enums.EstadoAsistencia;

import java.time.LocalDateTime;
import java.util.UUID;

public record AsistenciaResumenResponse(
        UUID id,
        UUID claseId,
        String claseTitulo,
        LocalDateTime claseFechaHora,
        UUID estudianteId,
        String estudianteNombreCompleto,
        EstadoAsistencia estado,
        LocalDateTime registradoAt
) {
}