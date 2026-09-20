package com.kellyacademy.shared.util;

import com.kellyacademy.security.user.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

// Helper estatico para leer el usuario autenticado desde el SecurityContext.
// El principal SIEMPRE es CustomUserDetails (lo garantiza JwtAuthenticationFilter),
// por lo que el cast es seguro dentro del flujo HTTP autenticado.

public final class SecurityUtils {

    private static final String ROL_ADMINISTRADOR = "ROLE_ADMINISTRADOR";

    private SecurityUtils() {
        // Clase de utilidades: no instanciable.
    }

    public static CustomUserDetails getUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails details)) {
            throw new IllegalStateException(
                    "No hay usuario autenticado en el SecurityContext. "
                            + "Este metodo solo debe invocarse dentro de un request autenticado."
            );
        }
        return details;
    }

    public static UUID getUsuarioAutenticadoId() {
        return getUsuarioAutenticado().getUsuario().getId();
    }

    public static boolean esAdmin() {
        return getUsuarioAutenticado().getAuthorities().stream()
                .anyMatch(a -> ROL_ADMINISTRADOR.equals(a.getAuthority()));
    }

    public static boolean esElMismoUsuario(UUID id) {
        return getUsuarioAutenticadoId().equals(id);
    }
}