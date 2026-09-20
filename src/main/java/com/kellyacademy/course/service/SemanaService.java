package com.kellyacademy.course.service;

import com.kellyacademy.course.dto.request.ActualizarSemanaRequest;
import com.kellyacademy.course.dto.request.CrearSemanaRequest;
import com.kellyacademy.course.dto.response.SemanaResponse;
import com.kellyacademy.course.dto.response.SemanaResumenResponse;
import com.kellyacademy.course.entity.Semana;
import com.kellyacademy.course.entity.Unidad;
import com.kellyacademy.course.mapper.SemanaMapper;
import com.kellyacademy.course.repository.ClaseRepository;
import com.kellyacademy.course.repository.MaterialRepository;
import com.kellyacademy.course.repository.SemanaRepository;
import com.kellyacademy.course.repository.TareaRepository;
import com.kellyacademy.course.repository.UnidadRepository;
import com.kellyacademy.shared.exception.BusinessException;
import com.kellyacademy.shared.exception.ResourceNotFoundException;
import com.kellyacademy.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SemanaService {

    private static final String RECURSO = "Semana";
    private static final String RECURSO_UNIDAD = "Unidad";

    private final SemanaRepository semanaRepository;
    private final UnidadRepository unidadRepository;
    private final ClaseRepository claseRepository;
    private final MaterialRepository materialRepository;
    private final TareaRepository tareaRepository;
    private final SemanaMapper semanaMapper;

    @Transactional(readOnly = true)
    public List<SemanaResumenResponse> listar(UUID unidadId) {
        if (unidadId == null) {
            throw new BusinessException(
                    "UNIDAD_ID_REQUERIDO",
                    "El filtro unidadId es obligatorio para listar semanas"
            );
        }
        if (!unidadRepository.existsById(unidadId)) {
            throw new ResourceNotFoundException(RECURSO_UNIDAD, "id", unidadId);
        }
        return semanaRepository.findByUnidadIdOrderByNumeroAsc(unidadId).stream()
                .map(semanaMapper::toResumenResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SemanaResponse obtener(UUID id) {
        Semana semana = semanaRepository.findWithUnidadCursoDocenteById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RECURSO, "id", id));
        return semanaMapper.toResponse(semana);
    }

    public SemanaResponse crear(CrearSemanaRequest request) {

        Unidad unidad = unidadRepository.findWithCursoDocenteById(request.unidadId())
                .orElseThrow(() -> new ResourceNotFoundException(RECURSO_UNIDAD, "id", request.unidadId()));

        validarPropietarioOAdmin(unidad);

        if (semanaRepository.existsByUnidadIdAndNumero(request.unidadId(), request.numero())) {
            throw new BusinessException(
                    "NUMERO_DUPLICADO",
                    "Ya existe una semana con el numero " + request.numero() + " en esta unidad"
            );
        }

        Semana semana = semanaMapper.toEntity(request);
        semana.setUnidad(unidad);
        semana.setEsActual(false);

        Semana guardada = semanaRepository.save(semana);

        return semanaMapper.toResponse(
                semanaRepository.findWithUnidadCursoDocenteById(guardada.getId()).orElseThrow()
        );
    }

    public SemanaResponse actualizar(UUID id, ActualizarSemanaRequest request) {

        Semana semana = semanaRepository.findWithUnidadCursoDocenteById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RECURSO, "id", id));

        validarPropietarioOAdmin(semana.getUnidad());

        semanaMapper.actualizarDesdeRequest(request, semana);

        return semanaMapper.toResponse(semana);
    }

    public void eliminar(UUID id) {

        Semana semana = semanaRepository.findWithUnidadCursoDocenteById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RECURSO, "id", id));

        validarPropietarioOAdmin(semana.getUnidad());

        // No permitimos borrar una semana con contenido: la FK saltaria como 409 generico.
        if (claseRepository.existsBySemanaId(id)
                || materialRepository.existsBySemanaId(id)
                || tareaRepository.existsBySemanaId(id)) {
            throw new BusinessException(
                    "SEMANA_CON_CONTENIDO",
                    "No se puede eliminar la semana porque tiene clases, materiales o tareas asociadas"
            );
        }

        semanaRepository.delete(semana);
    }

    // Marca la semana como actual y desmarca cualquier otra que estuviera marcada
    // en la misma unidad. Transaccional: o ambas cosas ocurren, o ninguna.
    // Idempotente: si ya es actual, no hace nada y devuelve 200.
    public SemanaResponse marcarActual(UUID id) {

        Semana semana = semanaRepository.findWithUnidadCursoDocenteById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RECURSO, "id", id));

        validarPropietarioOAdmin(semana.getUnidad());

        if (Boolean.TRUE.equals(semana.getEsActual())) {
            return semanaMapper.toResponse(semana);
        }

        semanaRepository.findByUnidadIdAndEsActualTrue(semana.getUnidad().getId())
                .ifPresent(actual -> actual.setEsActual(false));

        semana.setEsActual(true);

        return semanaMapper.toResponse(semana);
    }

    private void validarPropietarioOAdmin(Unidad unidad) {
        if (SecurityUtils.esAdmin()) {
            return;
        }
        UUID docenteId = unidad.getCurso().getDocente().getId();
        if (!docenteId.equals(SecurityUtils.getUsuarioAutenticadoId())) {
            throw new AccessDeniedException("No tienes permisos para modificar esta semana");
        }
    }
}