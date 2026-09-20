package com.kellyacademy.user.repository;

import com.kellyacademy.user.entity.Usuario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    // Carga el grafo completo necesario para construir CustomUserDetails
    // (roles + permisos) en una sola query. Sin esto, el login revienta
    // con LazyInitializationException porque loadUserByUsername corre
    // fuera de transaccion (open-in-view=false).
    @EntityGraph(attributePaths = {"roles", "roles.permisos"})
    Optional<Usuario> findByCorreoElectronico(String correoElectronico);

    // Carga solo el grafo "roles" (un nivel) para construir UsuarioResponse
    // (que expone nombres de roles). No cargamos "roles.permisos" porque
    // UsuarioMapper.nombresDeRoles solo lee Rol.getNombre().
    // @Query explicito: el nombre "findWithRolesById" no es derivable por Spring Data.
    @EntityGraph(attributePaths = {"roles"})
    @Query("SELECT u FROM Usuario u WHERE u.id = :id")
    Optional<Usuario> findWithRolesById(@Param("id") UUID id);

    boolean existsByCorreoElectronico(String correoElectronico);

}