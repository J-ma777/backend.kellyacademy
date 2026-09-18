package com.kellyacademy.user.dto.response;

import java.util.UUID;

public record PermisoResponse(
        UUID id,
        String nombre,
        String descripcion
) {
}