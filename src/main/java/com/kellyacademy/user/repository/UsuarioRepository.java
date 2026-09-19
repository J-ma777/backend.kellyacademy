package com.kellyacademy.user.repository;

import com.kellyacademy.user.entity.Usuario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    // Carga el grafo completo necesario para construir CustomUserDetails
    // (roles + permisos) en una sola query. Sin esto, el login revienta
    // con LazyInitializationException porque loadUserByUsername corre
    // fuera de transaccion (open-in-view=false).
    @EntityGraph(attributePaths = {"roles", "roles.permisos"})
    Optional<Usuario> findByCorreoElectronico(String correoElectronico);

    boolean existsByCorreoElectronico(String correoElectronico);

}