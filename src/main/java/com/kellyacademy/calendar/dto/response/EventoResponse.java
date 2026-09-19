package com.kellyacademy.calendar.dto.response;

import com.kellyacademy.calendar.enums.TipoEvento;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventoResponse(
        UUID id,
        UUID usuarioId,
        String usuarioNombreCompleto,
        UUID cursoId,
        String cursoTitulo,
        String titulo,
        String descripcion,
        TipoEvento tipo,
        LocalDateTime inicio,
        LocalDateTime fin,
        String sala,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion
) {
}