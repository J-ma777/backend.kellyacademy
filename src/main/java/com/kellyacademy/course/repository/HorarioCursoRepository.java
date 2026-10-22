package com.kellyacademy.course.repository;
import com.kellyacademy.course.entity.HorarioCurso;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface HorarioCursoRepository extends JpaRepository<HorarioCurso,UUID> {
    @EntityGraph(attributePaths={"curso","curso.docente"})
    List<HorarioCurso> findByCursoIdIn(Collection<UUID> ids);
    @EntityGraph(attributePaths={"curso","curso.docente"})
    @Query("SELECT h FROM HorarioCurso h WHERE h.curso.docente.id=:docenteId AND h.curso.estado=com.kellyacademy.course.enums.EstadoCurso.ACTIVO")
    List<HorarioCurso> findActivosDelDocente(@Param("docenteId") UUID docenteId);
}
