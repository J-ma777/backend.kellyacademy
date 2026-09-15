package com.kellyacademy.enrollment.repository;

import com.kellyacademy.enrollment.entity.Matricula;
import com.kellyacademy.enrollment.enums.EstadoMatricula;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MatriculaRepository extends JpaRepository<Matricula, UUID> {

    List<Matricula> findByCursoId(UUID cursoId);

    List<Matricula> findByEstudianteId(UUID estudianteId);

    Optional<Matricula> findByCursoIdAndEstudianteId(UUID cursoId, UUID estudianteId);

    boolean existsByCursoIdAndEstudianteId(UUID cursoId, UUID estudianteId);

    List<Matricula> findByCursoIdAndEstado(UUID cursoId, EstadoMatricula estado);
}