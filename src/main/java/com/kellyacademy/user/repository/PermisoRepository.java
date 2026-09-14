package com.kellyacademy.user.repository;

import com.kellyacademy.user.entity.Permiso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PermisoRepository extends JpaRepository<Permiso, UUID> {

    Optional<Permiso> findByNombre(String nombre);

}