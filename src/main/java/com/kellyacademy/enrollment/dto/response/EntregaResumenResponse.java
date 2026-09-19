package com.kellyacademy.enrollment.dto.response;

import com.kellyacademy.enrollment.enums.EstadoEntrega;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record EntregaResumenResponse(
        UUID id,
        UUID tareaId,
        String tareaTitulo,
        UUID estudianteId,
        String estudianteNombreCompleto,
        BigDecimal nota,
        EstadoEntrega estado,
        LocalDateTime enviadoAt
) {
}