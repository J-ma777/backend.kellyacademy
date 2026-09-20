package com.kellyacademy.course.service;

import com.kellyacademy.course.dto.request.ActualizarTareaRequest;
import com.kellyacademy.course.dto.request.CrearTareaRequest;
import com.kellyacademy.course.dto.response.TareaResponse;
import com.kellyacademy.course.dto.response.TareaResumenResponse;
import com.kellyacademy.course.entity.Semana;
import com.kellyacademy.course.entity.Tarea;
import com.kellyacademy.course.mapper.TareaMapper;
import com.kellyacademy.course.repository.SemanaRepository;
import com.kellyacademy.course.repository.TareaRepository;
import com.kellyacademy.shared.exception.BusinessException;
import com.kellyacademy.shared.exception.ResourceNotFoundException;
import com.kellyacademy.shared.util.SecurityUtils;
import com.kellyacademy.shared.util.UrlValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class TareaService {

    private final TareaRepository tareaRepository;
    private final SemanaRepository semanaRepository;
    private final TareaMapper tareaMapper;
    private static final String RECURSO = "Tarea";
    private static final String RECURSO_SEMANA = "Semana";

    @Transactional(readOnly = true)
    public List<TareaResumenResponse> listar(UUID semanaId) {
        if (semanaId == null) {
            throw new BusinessException(
                    "SEMANA_ID_REQUERIDO",
                    "El filtro semanaId es obligatorio para listar tareas"
            );
        }
        if (!semanaRepository.existsById(semanaId)) {
            throw new ResourceNotFoundException(RECURSO_SEMANA, "id", semanaId);
        }
        return tareaRepository.findBySemanaIdOrderByFechaLimiteAsc(semanaId).stream()
                .map(tareaMapper::toResumenResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TareaResponse obtener(UUID id) {
        Tarea tarea = tareaRepository.findWithSemanaCursoDocenteById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RECURSO, "id", id));
        return tareaMapper.toResponse(tarea);
    }

    public TareaResponse crear(CrearTareaRequest request) {

        Semana semana = semanaRepository.findWithUnidadCursoDocenteById(request.semanaId())
                .orElseThrow(() -> new ResourceNotFoundException(RECURSO_SEMANA, "id", request.semanaId()));

        validarAutorizacion(semana);

        validarUrl(request.instruccionesUrl());

        Tarea tarea = tareaMapper.toEntity(request);
        tarea.setSemana(semana);

        Tarea guardada = tareaRepository.save(tarea);

        return tareaMapper.toResponse(
                tareaRepository.findWithSemanaCursoDocenteById(guardada.getId()).orElseThrow()
        );
    }

    public TareaResponse actualizar(UUID id, ActualizarTareaRequest request) {

        Tarea tarea = tareaRepository.findWithSemanaCursoDocenteById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RECURSO, "id", id));

        validarAutorizacion(tarea.getSemana());

        validarUrl(request.instruccionesUrl());

        tareaMapper.actualizarDesdeRequest(request, tarea);

        return tareaMapper.toResponse(tarea);
    }

    public void eliminar(UUID id) {

        Tarea tarea = tareaRepository.findWithSemanaCursoDocenteById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RECURSO, "id", id));

        validarAutorizacion(tarea.getSemana());

        // Si tiene Entregas asociadas, la FK salta como 409 via DataIntegrityViolationException.
        // Cuando lleguemos a enrollment, si se necesita mensaje mas claro, se valida explicitamente.
        tareaRepository.delete(tarea);
    }

    private void validarAutorizacion(Semana semana) {
        UUID docenteId = semana.getUnidad().getCurso().getDocente().getId();
        SecurityUtils.validarDocenteDuenoOAdmin(
                docenteId,
                "No tienes permisos para modificar esta tarea"
        );
    }

    private void validarUrl(String url) {
        if (!UrlValidator.esFormatoValido(url)) {
            throw new BusinessException(
                    "URL_INVALIDA",
                    "La URL de instrucciones no tiene un formato valido: " + url
            );
        }
    }
}