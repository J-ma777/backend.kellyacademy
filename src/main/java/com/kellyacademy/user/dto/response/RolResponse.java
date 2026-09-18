package com.kellyacademy.user.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record RolResponse(
        UUID id,
        String nombre,
        String descripcion,
        List<PermisoResumenResponse> permisos,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion
) {
}