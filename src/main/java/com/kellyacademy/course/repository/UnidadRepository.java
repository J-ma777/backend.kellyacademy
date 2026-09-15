package com.kellyacademy.course.repository;

import com.kellyacademy.course.entity.Unidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UnidadRepository extends JpaRepository<Unidad, UUID> {

    List<Unidad> findByCursoIdOrderByNumeroAsc(UUID cursoId);

    Optional<Unidad> findByCursoIdAndNumero(UUID cursoId, Integer numero);

    boolean existsByCursoIdAndNumero(UUID cursoId, Integer numero);
}