package com.kellyacademy.user.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

/*
 Request administrativo para reemplazar los roles de un usuario.
 Endpoint dedicado: PUT /api/usuarios/{id}/roles.
 Semantica de reemplazo total: el set enviado es el estado final deseado.
 No vacio: un usuario sin roles no puede autenticarse con sentido.
 */
public record AsignarRolesUsuarioRequest(

        @NotEmpty(message = "Debe enviar al menos un rol")
        @Size(max = 10, message = "No se pueden asignar mas de 10 roles a un usuario")
        Set<String> roles
) {
}