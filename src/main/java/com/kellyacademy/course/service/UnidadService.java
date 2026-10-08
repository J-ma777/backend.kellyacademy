package com.kellyacademy.course.service;

import com.kellyacademy.course.dto.request.ActualizarUnidadRequest;
import com.kellyacademy.course.dto.request.CrearUnidadRequest;
import com.kellyacademy.course.dto.request.ReordenarRequest;
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
import java.util.Map;
import java.util.Set;
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

    public List<UnidadResumenResponse> reordenar(UUID cursoId, ReordenarRequest request) {
        Curso curso = cursoRepository.findWithDocenteById(cursoId)
                .orElseThrow(() -> new ResourceNotFoundException(RECURSO_CURSO, "id", cursoId));

        validarPropietarioOAdmin(curso);

        List<Unidad> unidades = unidadRepository.findByCursoIdOrderByNumeroAsc(cursoId);
        validarOrden(request, unidades);

        Map<UUID, Integer> numerosPorId = request.orden().stream()
                .collect(java.util.stream.Collectors.toMap(
                        ReordenarRequest.ItemOrden::id,
                        ReordenarRequest.ItemOrden::numero
                ));

        // Offset temporal: evita colisiones intermedias con la restriccion UNIQUE.
        unidades.forEach(unidad -> unidad.setNumero(unidad.getNumero() + 1_000_000));
        unidadRepository.saveAll(unidades);
        unidadRepository.flush();

        unidades.forEach(unidad -> unidad.setNumero(numerosPorId.get(unidad.getId())));
        unidadRepository.saveAll(unidades);
        unidadRepository.flush();

        return unidadRepository.findByCursoIdOrderByNumeroAsc(cursoId).stream()
                .map(unidadMapper::toResumenResponse)
                .toList();
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

    private void validarOrden(ReordenarRequest request, List<Unidad> unidades) {
        if (request == null || request.orden() == null || request.orden().isEmpty()) {
            throw new BusinessException("ORDEN_VACIO", "El orden no puede estar vacio");
        }

        Set<UUID> idsActuales = unidades.stream()
                .map(Unidad::getId)
                .collect(java.util.stream.Collectors.toSet());
        Set<UUID> idsEnviados = request.orden().stream()
                .map(ReordenarRequest.ItemOrden::id)
                .collect(java.util.stream.Collectors.toSet());

        if (!idsEnviados.containsAll(idsActuales)) {
            throw new BusinessException("ORDEN_INCOMPLETO", "El orden debe incluir todas las unidades del curso");
        }
        if (!idsActuales.containsAll(idsEnviados)) {
            throw new BusinessException("ORDEN_CON_IDS_AJENOS", "El orden contiene unidades que no pertenecen al curso");
        }

        Set<Integer> numeros = request.orden().stream()
                .map(ReordenarRequest.ItemOrden::numero)
                .collect(java.util.stream.Collectors.toSet());
        if (numeros.size() != request.orden().size()) {
            throw new BusinessException("ORDEN_CON_NUMEROS_DUPLICADOS", "El orden contiene numeros duplicados");
        }
        for (int numero = 1; numero <= unidades.size(); numero++) {
            if (!numeros.contains(numero)) {
                throw new BusinessException(
                        "ORDEN_CON_NUMEROS_NO_CONTIGUOS",
                        "Los numeros deben formar un rango contiguo del 1 al " + unidades.size()
                );
            }
        }

        List<UUID> ordenActual = unidades.stream().map(Unidad::getId).toList();
        List<UUID> ordenNuevo = request.orden().stream()
                .sorted(java.util.Comparator.comparing(ReordenarRequest.ItemOrden::numero))
                .map(ReordenarRequest.ItemOrden::id)
                .toList();
        if (ordenActual.equals(ordenNuevo)) {
            throw new BusinessException("ORDEN_SIN_CAMBIOS", "El orden ya es el mismo");
        }
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