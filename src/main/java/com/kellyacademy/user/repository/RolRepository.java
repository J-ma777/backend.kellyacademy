package com.kellyacademy.user.repository;

import com.kellyacademy.user.entity.Rol;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RolRepository extends JpaRepository<Rol, UUID> {

    // Carga permisos junto con el rol para que RolMapper.toResponse pueda
    // mapearlos fuera de transaccion sin LazyInitializationException.
    @EntityGraph(attributePaths = {"permisos"})
    Optional<Rol> findByNombre(String nombre);

}