package com.kellyacademy.course.service;

import com.kellyacademy.course.dto.request.ActualizarSemanaRequest;
import com.kellyacademy.course.dto.request.CrearSemanaRequest;
import com.kellyacademy.course.dto.request.ReordenarRequest;
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
import java.util.Map;
import java.util.Set;
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

    public List<SemanaResumenResponse> reordenar(UUID unidadId, ReordenarRequest request) {
        Unidad unidad = unidadRepository.findWithCursoDocenteById(unidadId)
                .orElseThrow(() -> new ResourceNotFoundException(RECURSO_UNIDAD, "id", unidadId));

        validarPropietarioOAdmin(unidad);

        List<Semana> semanas = semanaRepository.findByUnidadIdOrderByNumeroAsc(unidadId);
        validarOrden(request, semanas);

        Map<UUID, Integer> numerosPorId = request.orden().stream()
                .collect(java.util.stream.Collectors.toMap(
                        ReordenarRequest.ItemOrden::id,
                        ReordenarRequest.ItemOrden::numero
                ));

        // Offset temporal: evita colisiones intermedias con la restriccion UNIQUE.
        semanas.forEach(semana -> semana.setNumero(semana.getNumero() + 1_000_000));
        semanaRepository.saveAll(semanas);
        semanaRepository.flush();

        semanas.forEach(semana -> semana.setNumero(numerosPorId.get(semana.getId())));
        semanaRepository.saveAll(semanas);
        semanaRepository.flush();

        return semanaRepository.findByUnidadIdOrderByNumeroAsc(unidadId).stream()
                .map(semanaMapper::toResumenResponse)
                .toList();
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

    private void validarOrden(ReordenarRequest request, List<Semana> semanas) {
        if (request == null || request.orden() == null || request.orden().isEmpty()) {
            throw new BusinessException("ORDEN_VACIO", "El orden no puede estar vacio");
        }

        Set<UUID> idsActuales = semanas.stream()
                .map(Semana::getId)
                .collect(java.util.stream.Collectors.toSet());
        Set<UUID> idsEnviados = request.orden().stream()
                .map(ReordenarRequest.ItemOrden::id)
                .collect(java.util.stream.Collectors.toSet());

        if (!idsEnviados.containsAll(idsActuales)) {
            throw new BusinessException("ORDEN_INCOMPLETO", "El orden debe incluir todas las semanas de la unidad");
        }
        if (!idsActuales.containsAll(idsEnviados)) {
            throw new BusinessException("ORDEN_CON_IDS_AJENOS", "El orden contiene semanas que no pertenecen a la unidad");
        }

        Set<Integer> numeros = request.orden().stream()
                .map(ReordenarRequest.ItemOrden::numero)
                .collect(java.util.stream.Collectors.toSet());
        if (numeros.size() != request.orden().size()) {
            throw new BusinessException("ORDEN_CON_NUMEROS_DUPLICADOS", "El orden contiene numeros duplicados");
        }
        for (int numero = 1; numero <= semanas.size(); numero++) {
            if (!numeros.contains(numero)) {
                throw new BusinessException(
                        "ORDEN_CON_NUMEROS_NO_CONTIGUOS",
                        "Los numeros deben formar un rango contiguo del 1 al " + semanas.size()
                );
            }
        }

        List<UUID> ordenActual = semanas.stream().map(Semana::getId).toList();
        List<UUID> ordenNuevo = request.orden().stream()
                .sorted(java.util.Comparator.comparing(ReordenarRequest.ItemOrden::numero))
                .map(ReordenarRequest.ItemOrden::id)
                .toList();
        if (ordenActual.equals(ordenNuevo)) {
            throw new BusinessException("ORDEN_SIN_CAMBIOS", "El orden ya es el mismo");
        }
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