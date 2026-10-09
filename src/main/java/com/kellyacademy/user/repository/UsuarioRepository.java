package com.kellyacademy.user.repository;

import com.kellyacademy.user.entity.Usuario;
import com.kellyacademy.user.enums.EstadoUsuario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    @EntityGraph(attributePaths = {"roles", "roles.permisos"})
    Optional<Usuario> findByCorreoElectronico(String correoElectronico);

    @EntityGraph(attributePaths = {"roles"})
    @Query("SELECT u FROM Usuario u WHERE u.id = :id")
    Optional<Usuario> findWithRolesById(@Param("id") UUID id);

    boolean existsByCorreoElectronico(String correoElectronico);

    // Cuenta cuantos usuarios tienen un rol (por nombre) y estado concretos.
    // Usado por la proteccion del ultimo admin activo (#70):
    // countByRolNombreAndEstado("ADMINISTRADOR", ACTIVO) devuelve el total de admins operativos.
    // JOIN explicito (no navegacion) para evitar N+1 y funcionar con roles LAZY.
    @Query("""
        SELECT COUNT(u) FROM Usuario u
        JOIN u.roles r
        WHERE r.nombre = :rolNombre
          AND u.estado = :estado
    """)
    long countByRolNombreAndEstado(@Param("rolNombre") String rolNombre,
                                   @Param("estado") EstadoUsuario estado);
}