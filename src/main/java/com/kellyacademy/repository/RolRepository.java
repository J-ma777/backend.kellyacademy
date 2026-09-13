package com.kellyacademy.repository;

import com.kellyacademy.model.usuario.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RolRepository extends JpaRepository<Rol, UUID> {

    Optional<Rol> findByNombre(String nombre);

}