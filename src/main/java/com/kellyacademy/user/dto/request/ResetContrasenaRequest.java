package com.kellyacademy.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/*
 Request para el reset administrativo de contrasena.
 Endpoint dedicado: POST /api/usuarios/{id}/reset-contrasena (solo ADMINISTRADOR).

 A diferencia de CambiarContrasenaRequest, NO requiere la contrasena actual:
 el admin que ejecuta el reset no la conoce. Este endpoint existe precisamente
 para el flujo de "usuario olvido su contrasena" o "admin reasigna cuentas".

 Reglas de fortaleza: identicas a CrearUsuarioRequest y CambiarContrasenaRequest.
 */
public record ResetContrasenaRequest(

        @NotBlank(message = "La contrasena nueva es obligatoria")
        @Size(min = 8, max = 72, message = "La contrasena debe tener entre 8 y 72 caracteres")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                message = "La contrasena debe contener al menos una letra y un numero"
        )
        String contrasenaNueva
) {
}