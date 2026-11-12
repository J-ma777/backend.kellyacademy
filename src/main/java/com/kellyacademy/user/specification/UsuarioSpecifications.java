package com.kellyacademy.user.specification;

import com.kellyacademy.user.entity.Usuario;
import com.kellyacademy.user.enums.EstadoUsuario;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

public final class UsuarioSpecifications {
    private UsuarioSpecifications() {}

    public static Specification<Usuario> filtros(String texto, EstadoUsuario estado, String rol) {
        return Specification.allOf(texto(texto), estado(estado), rol(rol));
    }

    private static Specification<Usuario> texto(String texto) {
        if (texto == null || texto.isBlank()) return Specification.unrestricted();
        String busqueda = texto.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        String patron = "%" + busqueda + "%";
        return (raiz, consulta, cb) -> cb.or(
                cb.like(cb.lower(raiz.get("nombre")), patron, '\\'),
                cb.like(cb.lower(raiz.get("apellido")), patron, '\\'),
                cb.like(cb.lower(raiz.get("correoElectronico")), patron, '\\'),
                cb.like(cb.lower(cb.concat(cb.concat(raiz.get("nombre"), " "), raiz.get("apellido"))), patron, '\\')
        );
    }

    private static Specification<Usuario> estado(EstadoUsuario estado) {
        if (estado == null) return Specification.unrestricted();
        return (raiz, consulta, cb) -> cb.equal(raiz.get("estado"), estado);
    }

    private static Specification<Usuario> rol(String rol) {
        if (rol == null || rol.isBlank()) return Specification.unrestricted();
        String nombreRol = rol.trim().toUpperCase(Locale.ROOT);
        return (raiz, consulta, cb) -> cb.equal(raiz.join("roles").get("nombre"), nombreRol);
    }
}
