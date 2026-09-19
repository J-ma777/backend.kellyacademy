package com.kellyacademy.enrollment.dto.response;

import com.kellyacademy.enrollment.enums.EstadoEntrega;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record EntregaResponse(
        UUID id,
        UUID tareaId,
        String tareaTitulo,
        UUID estudianteId,
        String estudianteNombreCompleto,
        String estudianteCorreo,
        String urlArchivo,
        LocalDateTime enviadoAt,
        BigDecimal nota,
        String retroalimentacion,
        EstadoEntrega estado,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion
) {
}