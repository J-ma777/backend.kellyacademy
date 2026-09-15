package com.kellyacademy.attendance.repository;

import com.kellyacademy.attendance.entity.Asistencia;
import com.kellyacademy.attendance.enums.EstadoAsistencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AsistenciaRepository extends JpaRepository<Asistencia, UUID> {

    List<Asistencia> findByClaseId(UUID claseId);

    List<Asistencia> findByEstudianteId(UUID estudianteId);

    Optional<Asistencia> findByClaseIdAndEstudianteId(UUID claseId, UUID estudianteId);

    List<Asistencia> findByClaseIdAndEstado(UUID claseId, EstadoAsistencia estado);

    long countByEstudianteIdAndEstado(UUID estudianteId, EstadoAsistencia estado);

    long countByEstudianteId(UUID estudianteId);
}