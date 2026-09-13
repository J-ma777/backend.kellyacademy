package com.kellyacademy.repository;

import com.kellyacademy.model.usuario.Permiso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PermisoRepository extends JpaRepository<Permiso, UUID> {

    Optional<Permiso> findByNombre(String nombre);

}