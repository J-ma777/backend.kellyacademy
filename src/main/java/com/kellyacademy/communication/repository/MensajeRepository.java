package com.kellyacademy.communication.repository;

import com.kellyacademy.communication.entity.Mensaje;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MensajeRepository extends JpaRepository<Mensaje, UUID> {

    List<Mensaje> findByConversacionIdOrderByEnviadoAtAsc(UUID conversacionId);

    long countByConversacionIdAndLeidoFalse(UUID conversacionId);

    long countByConversacionIdAndRemitenteIdNotAndLeidoFalse(UUID conversacionId, UUID remitenteId);
}