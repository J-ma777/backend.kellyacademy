package com.kellyacademy.course.repository;

import com.kellyacademy.course.entity.Material;
import com.kellyacademy.course.enums.TipoMaterial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MaterialRepository extends JpaRepository<Material, UUID> {

    List<Material> findBySemanaId(UUID semanaId);

    List<Material> findBySemanaIdAndTipo(UUID semanaId, TipoMaterial tipo);
}