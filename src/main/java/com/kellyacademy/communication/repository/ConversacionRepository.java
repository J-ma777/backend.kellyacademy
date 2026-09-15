package com.kellyacademy.communication.repository;

import com.kellyacademy.communication.entity.Conversacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversacionRepository extends JpaRepository<Conversacion, UUID> {

    List<Conversacion> findByParticipante1IdOrParticipante2IdOrderByUltimoMensajeAtDesc(UUID id1, UUID id2);

    Optional<Conversacion> findByCursoIdAndParticipante1IdAndParticipante2Id(UUID cursoId, UUID p1, UUID p2);
}