package com.kellyacademy.course.repository;

import com.kellyacademy.course.entity.Clase;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClaseRepository extends JpaRepository<Clase, UUID> {

    @EntityGraph(attributePaths = {"semana"})
    List<Clase> findBySemanaUnidadCursoIdOrderByFechaHoraAsc(UUID cursoId);

    List<Clase> findBySemanaIdOrderByFechaHoraAsc(UUID semanaId);

    @EntityGraph(attributePaths = {"semana", "semana.unidad", "semana.unidad.curso", "semana.unidad.curso.docente"})
    List<Clase> findByFechaHoraBetween(LocalDateTime inicio, LocalDateTime fin);

    boolean existsBySemanaId(UUID semanaId);

    // Carga el grafo completo hasta el docente del curso para que ClaseMapper
    // (lee semana.getId()) y la validacion de autorizacion (semana.unidad.curso.docente.id)
    // funcionen sin LazyInitializationException con open-in-view=false.
    @EntityGraph(attributePaths = {
            "semana",
            "semana.unidad",
            "semana.unidad.curso",
            "semana.unidad.curso.docente"
    })
    @Query("SELECT c FROM Clase c WHERE c.id = :id")
    Optional<Clase> findWithSemanaCursoDocenteById(@Param("id") UUID id);

    interface ConteoPorCurso {
        UUID getCursoId();
        Long getTotal();
    }

    @Query("""
        SELECT c.semana.unidad.curso.id AS cursoId, COUNT(c) AS total
        FROM Clase c
        WHERE c.semana.unidad.curso.id IN :cursoIds
          AND c.fechaHora IS NOT NULL AND c.fechaHora <= :ahora
        GROUP BY c.semana.unidad.curso.id
    """)
    List<ConteoPorCurso> contarDictadasPorCursos(@Param("cursoIds") java.util.Collection<UUID> cursoIds,
                                               @Param("ahora") LocalDateTime ahora);

    @Query("""
        SELECT COUNT(c) FROM Clase c
        WHERE c.semana.unidad.curso.id = :cursoId
          AND c.fechaHora IS NOT NULL
          AND c.fechaHora <= :ahora
    """)
    long contarClasesDictadasPorCurso(@Param("cursoId") UUID cursoId, @Param("ahora") LocalDateTime ahora);
}
