package com.kellyacademy.user.repository;

import com.kellyacademy.user.entity.Rol;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RolRepository extends JpaRepository<Rol, UUID> {

    // Carga permisos junto con el rol para que RolMapper.toResponse pueda
    // mapearlos fuera de transaccion sin LazyInitializationException.
    @EntityGraph(attributePaths = {"permisos"})
    Optional<Rol> findByNombre(String nombre);

    // Carga permisos para construir RolResponse (que itera permisos).
    // @Query explicito: el nombre "findWithPermisosById" no es derivable por Spring Data.
    @EntityGraph(attributePaths = {"permisos"})
    @Query("SELECT r FROM Rol r WHERE r.id = :id")
    Optional<Rol> findWithPermisosById(@Param("id") UUID id);

}