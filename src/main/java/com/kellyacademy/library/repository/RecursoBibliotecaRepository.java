package com.kellyacademy.library.repository;

import com.kellyacademy.course.enums.NivelCefr;
import com.kellyacademy.course.enums.TipoMaterial;
import com.kellyacademy.library.entity.RecursoBiblioteca;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RecursoBibliotecaRepository extends JpaRepository<RecursoBiblioteca, UUID> {

    List<RecursoBiblioteca> findByCategoria(String categoria);

    List<RecursoBiblioteca> findByNivelCefr(NivelCefr nivelCefr);

    List<RecursoBiblioteca> findByTipo(TipoMaterial tipo);

    List<RecursoBiblioteca> findByCategoriaAndNivelCefr(String categoria, NivelCefr nivelCefr);

    List<RecursoBiblioteca> findByTituloContainingIgnoreCase(String texto);
}