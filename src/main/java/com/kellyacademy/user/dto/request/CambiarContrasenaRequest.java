package com.kellyacademy.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/*
 Request para cambiar la contrasena del propio usuario autenticado.
 Endpoint dedicado: PATCH /api/usuarios/{id}/contrasena.

 Requiere la contrasena actual como prueba de identidad: si un atacante
 obtiene un token JWT valido pero no conoce la contrasena, no puede cambiarla
 sin conocer la actual. Esto obliga a que este endpoint solo lo pueda usar
 el propio usuario (nunca un ADMIN forzando reset: eso seria otro endpoint).

 Reglas de fortaleza: identicas a CrearUsuarioRequest.
 */
public record CambiarContrasenaRequest(

        @NotBlank(message = "La contrasena actual es obligatoria")
        String contrasenaActual,

        @NotBlank(message = "La contrasena nueva es obligatoria")
        @Size(min = 8, max = 72, message = "La contrasena debe tener entre 8 y 72 caracteres")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                message = "La contrasena debe contener al menos una letra y un numero"
        )
        String contrasenaNueva
) {
}