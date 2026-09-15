package com.kellyacademy.course.repository;

import com.kellyacademy.course.entity.Curso;
import com.kellyacademy.course.enums.EstadoCurso;
import com.kellyacademy.course.enums.NivelCefr;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CursoRepository extends JpaRepository<Curso, UUID> {

    List<Curso> findByDocenteId(UUID docenteId);

    List<Curso> findByEstado(EstadoCurso estado);

    List<Curso> findByNivelCefr(NivelCefr nivelCefr);

    List<Curso> findByDocenteIdAndEstado(UUID docenteId, EstadoCurso estado);
}