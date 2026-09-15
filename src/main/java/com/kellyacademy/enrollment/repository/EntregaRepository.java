package com.kellyacademy.enrollment.repository;

import com.kellyacademy.enrollment.entity.Entrega;
import com.kellyacademy.enrollment.enums.EstadoEntrega;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EntregaRepository extends JpaRepository<Entrega, UUID> {

    List<Entrega> findByTareaId(UUID tareaId);

    List<Entrega> findByEstudianteId(UUID estudianteId);

    Optional<Entrega> findByTareaIdAndEstudianteId(UUID tareaId, UUID estudianteId);

    List<Entrega> findByTareaIdAndEstado(UUID tareaId, EstadoEntrega estado);
}