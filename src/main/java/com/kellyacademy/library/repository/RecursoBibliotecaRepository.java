package com.kellyacademy.library.repository;

import com.kellyacademy.course.enums.NivelCefr;
import com.kellyacademy.course.enums.TipoMaterial;
import com.kellyacademy.library.entity.RecursoBiblioteca;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.UUID;

public interface RecursoBibliotecaRepository extends
        JpaRepository<RecursoBiblioteca, UUID>,
        JpaSpecificationExecutor<RecursoBiblioteca> {

    List<RecursoBiblioteca> findByCategoria(String categoria);

    List<RecursoBiblioteca> findByNivelCefr(NivelCefr nivelCefr);

    List<RecursoBiblioteca> findByTipo(TipoMaterial tipo);

    List<RecursoBiblioteca> findByCategoriaAndNivelCefr(String categoria, NivelCefr nivelCefr);

    List<RecursoBiblioteca> findByTituloContainingIgnoreCase(String texto);

    // RecursoBiblioteca no tiene @ManyToOne, todo es escalar.
    // No hace falta @EntityGraph. Pero mantenemos el override por convencion
    // (mismo patron que CursoRepository, MatriculaRepository, EventoRepository,
    // DisponibilidadTutoriaRepository, TutoriaRepository) para que si en el
    // futuro se agrega una relacion LAZY, el punto de extension ya exista.
    @Override
    Page<RecursoBiblioteca> findAll(@Nullable Specification<RecursoBiblioteca> spec, Pageable pageable);

    // Incremento atomico del contador. Evita race de "read-modify-write" en Java.
    // Devuelve el numero de filas afectadas: 0 = recurso no existe.
    @Modifying
    @Query("UPDATE RecursoBiblioteca r SET r.contadorDescargas = r.contadorDescargas + 1 WHERE r.id = :id")
    int incrementarContadorDescargas(@Param("id") UUID id);
}