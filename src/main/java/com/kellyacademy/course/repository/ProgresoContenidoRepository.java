package com.kellyacademy.course.repository;
import com.kellyacademy.course.entity.ProgresoContenido;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;
public interface ProgresoContenidoRepository extends JpaRepository<ProgresoContenido, UUID> {
    boolean existsByEstudianteIdAndMaterialId(UUID estudianteId, UUID materialId);
    boolean existsByEstudianteIdAndClaseId(UUID estudianteId, UUID claseId);
    @EntityGraph(attributePaths = {"estudiante", "material", "clase"})
    @Query("SELECT p FROM ProgresoContenido p LEFT JOIN p.material m LEFT JOIN m.semana ms LEFT JOIN ms.unidad mu LEFT JOIN p.clase c LEFT JOIN c.semana cs LEFT JOIN cs.unidad cu WHERE mu.curso.id = :cursoId OR cu.curso.id = :cursoId")
    List<ProgresoContenido> findPorCurso(@Param("cursoId") UUID cursoId);
}
