package com.kellyacademy.attendance.dto.response;

import com.kellyacademy.attendance.enums.EstadoAsistencia;

import java.time.LocalDateTime;
import java.util.UUID;

public record AsistenciaResponse(
        UUID id,
        UUID claseId,
        String claseTitulo,
        LocalDateTime claseFechaHora,
        UUID estudianteId,
        String estudianteNombreCompleto,
        String estudianteCorreo,
        EstadoAsistencia estado,
        String observacion,
        LocalDateTime registradoAt,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion
) {
}