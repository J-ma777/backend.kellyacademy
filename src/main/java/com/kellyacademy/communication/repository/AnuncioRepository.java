package com.kellyacademy.communication.repository;

import com.kellyacademy.communication.entity.Anuncio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AnuncioRepository extends JpaRepository<Anuncio, UUID> {

    List<Anuncio> findByCursoIdAndActivoTrueOrderByFechaCreacionDesc(UUID cursoId);

    List<Anuncio> findByAutorId(UUID autorId);
}