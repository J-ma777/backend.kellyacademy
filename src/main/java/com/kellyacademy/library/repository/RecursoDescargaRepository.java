package com.kellyacademy.library.repository;

import com.kellyacademy.library.entity.RecursoDescarga;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RecursoDescargaRepository extends JpaRepository<RecursoDescarga, UUID> {

    long countByRecursoId(UUID recursoId);
}