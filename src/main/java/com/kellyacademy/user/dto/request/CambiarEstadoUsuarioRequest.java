package com.kellyacademy.user.dto.request;

import com.kellyacademy.user.enums.EstadoUsuario;
import jakarta.validation.constraints.NotNull;

/*
 Request administrativo para cambiar el estado de un usuario.
 Endpoint dedicado: PATCH /api/usuarios/{id}/estado.
 El estado no va en ActualizarUsuarioRequest: es campo administrativo, no editable por el propio usuario.
 */
public record CambiarEstadoUsuarioRequest(

        @NotNull(message = "El estado es obligatorio")
        EstadoUsuario estado
) {
}