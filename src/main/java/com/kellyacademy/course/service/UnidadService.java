package com.kellyacademy.course.service;

import com.kellyacademy.course.dto.request.ActualizarUnidadRequest;
import com.kellyacademy.course.dto.request.CrearUnidadRequest;
import com.kellyacademy.course.dto.response.UnidadResponse;
import com.kellyacademy.course.dto.response.UnidadResumenResponse;
import com.kellyacademy.course.entity.Curso;
import com.kellyacademy.course.entity.Unidad;
import com.kellyacademy.course.mapper.UnidadMapper;
import com.kellyacademy.course.repository.CursoRepository;
import com.kellyacademy.course.repository.SemanaRepository;
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
public class UnidadService {

    private final UnidadRepository unidadRepository;
    private final CursoRepository cursoRepository;
    private final SemanaRepository semanaRepository;
    private final UnidadMapper unidadMapper;
    private static final String RECURSO = "Unidad";
    private static final String RECURSO_CURSO = "Curso";

    @Transactional(readOnly = true)
    public List<UnidadResumenResponse> listar(UUID cursoId) {
        if (cursoId == null) {
            throw new BusinessException(
                    "CURSO_ID_REQUERIDO",
                    "El filtro cursoId es obligatorio para listar unidades"
            );
        }
        if (!cursoRepository.existsById(cursoId)) {
            throw new ResourceNotFoundException(RECURSO_CURSO, "id", cursoId);
        }
        return unidadRepository.findByCursoIdOrderByNumeroAsc(cursoId).stream()
                .map(unidadMapper::toResumenResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UnidadResponse obtener(UUID id) {
        Unidad unidad = unidadRepository.findWithCursoDocenteById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RECURSO, "id", id));
        return unidadMapper.toResponse(unidad);
    }

    public UnidadResponse crear(CrearUnidadRequest request) {

        Curso curso = cursoRepository.findWithDocenteById(request.cursoId())
                .orElseThrow(() -> new ResourceNotFoundException(RECURSO_CURSO, "id", request.cursoId()));

        validarPropietarioOAdmin(curso);

        if (unidadRepository.existsByCursoIdAndNumero(request.cursoId(), request.numero())) {
            throw new BusinessException(
                    "NUMERO_DUPLICADO",
                    "Ya existe una unidad con el numero " + request.numero() + " en este curso"
            );
        }

        Unidad unidad = unidadMapper.toEntity(request);
        unidad.setCurso(curso);

        Unidad guardada = unidadRepository.save(unidad);

        return unidadMapper.toResponse(
                unidadRepository.findWithCursoDocenteById(guardada.getId()).orElseThrow()
        );
    }

    public UnidadResponse actualizar(UUID id, ActualizarUnidadRequest request) {

        Unidad unidad = unidadRepository.findWithCursoDocenteById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RECURSO, "id", id));

        validarPropietarioOAdmin(unidad.getCurso());

        unidadMapper.actualizarDesdeRequest(request, unidad);

        return unidadMapper.toResponse(unidad);
    }

    public void eliminar(UUID id) {

        Unidad unidad = unidadRepository.findWithCursoDocenteById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RECURSO, "id", id));

        validarPropietarioOAdmin(unidad.getCurso());

        if (semanaRepository.existsByUnidadId(id)) {
            throw new BusinessException(
                    "UNIDAD_CON_SEMANAS",
                    "No se puede eliminar la unidad porque tiene semanas asociadas"
            );
        }

        unidadRepository.delete(unidad);
    }

    private void validarPropietarioOAdmin(Curso curso) {
        if (SecurityUtils.esAdmin()) {
            return;
        }
        UUID docenteId = curso.getDocente().getId();
        if (!docenteId.equals(SecurityUtils.getUsuarioAutenticadoId())) {
            throw new AccessDeniedException("No tienes permisos para modificar esta unidad");
        }
    }
}