package com.kellyacademy.user.dto.response;

import com.kellyacademy.user.enums.EstadoUsuario;

import java.util.UUID;


// DTO ligero para listados paginados.
// NO incluye roles ni fechas de auditoria.

public record UsuarioResumenResponse(
        UUID id,
        String nombre,
        String apellido,
        String correoElectronico,
        String avatarUrl,
        EstadoUsuario estado
) {
}
