package com.kellyacademy.course.repository;

import com.kellyacademy.course.entity.Unidad;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UnidadRepository extends JpaRepository<Unidad, UUID> {

    List<Unidad> findByCursoIdOrderByNumeroAsc(UUID cursoId);

    Optional<Unidad> findByCursoIdAndNumero(UUID cursoId, Integer numero);

    boolean existsByCursoIdAndNumero(UUID cursoId, Integer numero);

    // Carga curso + docente en la misma query. Necesario para que UnidadMapper.toResponse
    // lea curso.getId() sin LazyInit y para que el servicio valide propietario del curso.
    @EntityGraph(attributePaths = {"curso", "curso.docente"})
    @Query("SELECT u FROM Unidad u WHERE u.id = :id")
    Optional<Unidad> findWithCursoDocenteById(@Param("id") UUID id);

    // Lock pesimista sobre la fila de la unidad. Serializa reordenamientos de
    // semanas concurrentes sobre la misma unidad. Mismo patron que
    // CursoRepository.findByIdForUpdate.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM Unidad u WHERE u.id = :id")
    Optional<Unidad> findByIdForUpdate(@Param("id") UUID id);
}