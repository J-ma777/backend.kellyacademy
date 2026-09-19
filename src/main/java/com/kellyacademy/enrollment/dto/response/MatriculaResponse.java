package com.kellyacademy.enrollment.dto.response;

import com.kellyacademy.enrollment.enums.EstadoMatricula;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record MatriculaResponse(
        UUID id,
        UUID cursoId,
        String cursoTitulo,
        UUID estudianteId,
        String estudianteNombreCompleto,
        String estudianteCorreo,
        EstadoMatricula estado,
        BigDecimal notaFinal,
        BigDecimal asistenciaPorcentaje,
        LocalDateTime matriculadoAt,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion
) {
}