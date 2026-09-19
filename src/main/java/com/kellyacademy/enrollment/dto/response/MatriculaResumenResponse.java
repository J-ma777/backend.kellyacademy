package com.kellyacademy.enrollment.dto.response;

import com.kellyacademy.enrollment.enums.EstadoMatricula;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record MatriculaResumenResponse(
        UUID id,
        UUID cursoId,
        String cursoTitulo,
        UUID estudianteId,
        String estudianteNombreCompleto,
        EstadoMatricula estado,
        BigDecimal notaFinal,
        LocalDateTime matriculadoAt
) {
}