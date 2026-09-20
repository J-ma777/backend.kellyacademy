package com.kellyacademy.enrollment.service;

import com.kellyacademy.course.entity.Curso;
import com.kellyacademy.course.entity.Tarea;
import com.kellyacademy.course.repository.TareaRepository;
import com.kellyacademy.enrollment.dto.request.ActualizarEntregaRequest;
import com.kellyacademy.enrollment.dto.request.CrearEntregaRequest;
import com.kellyacademy.enrollment.dto.response.EntregaResponse;
import com.kellyacademy.enrollment.dto.response.EntregaResumenResponse;
import com.kellyacademy.enrollment.entity.Entrega;
import com.kellyacademy.enrollment.enums.EstadoEntrega;
import com.kellyacademy.enrollment.mapper.EntregaMapper;
import com.kellyacademy.enrollment.repository.EntregaRepository;
import com.kellyacademy.enrollment.repository.MatriculaRepository;
import com.kellyacademy.enrollment.specification.EntregaSpecifications;
import com.kellyacademy.shared.config.AppTime;
import com.kellyacademy.shared.exception.BusinessException;
import com.kellyacademy.shared.exception.ResourceNotFoundException;
import com.kellyacademy.shared.util.SecurityUtils;
import com.kellyacademy.shared.util.UrlValidator;
import com.kellyacademy.user.entity.Usuario;
import com.kellyacademy.user.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.data.jpa.domain.Specification.allOf;

@Service
@RequiredArgsConstructor
@Transactional
public class EntregaService {

    private static final String RECURSO = "Entrega";
    private static final String RECURSO_TAREA = "Tarea";
    private static final String RECURSO_USUARIO = "Usuario";
    private static final String ROL_ESTUDIANTE = "ESTUDIANTE";

    private final EntregaRepository entregaRepository;
    private final TareaRepository tareaRepository;
    private final UsuarioRepository usuarioRepository;
    private final MatriculaRepository matriculaRepository;
    private final EntregaMapper entregaMapper;

    // Listado administrativo. Si el autenticado es DOCENTE, se filtra a sus cursos.
    // Si es ADMIN, ve todas.
    @Transactional(readOnly = true)
    public Page<EntregaResumenResponse> listar(
            UUID tareaId,
            UUID estudianteId,
            EstadoEntrega estado,
            Pageable pageable
    ) {
        UUID docenteFiltro = SecurityUtils.esAdmin()
                ? null
                : SecurityUtils.getUsuarioAutenticadoId();

        Specification<Entrega> spec = allOf(
                EntregaSpecifications.porTareaId(tareaId),
                EntregaSpecifications.porEstudianteId(estudianteId),
                EntregaSpecifications.porEstado(estado),
                EntregaSpecifications.porDocenteId(docenteFiltro)
        );

        return entregaRepository.findAll(spec, pageable)
                .map(entregaMapper::toResumenResponse);
    }

    // Entregas del estudiante autenticado. No expone entregas de otros.
    @Transactional(readOnly = true)
    public Page<EntregaResumenResponse> listarMisEntregas(Pageable pageable) {
        UUID estudianteId = SecurityUtils.getUsuarioAutenticadoId();

        Specification<Entrega> spec = allOf(
                EntregaSpecifications.porEstudianteId(estudianteId)
        );

        return entregaRepository.findAll(spec, pageable)
                .map(entregaMapper::toResumenResponse);
    }

    @Transactional(readOnly = true)
    public EntregaResponse obtener(UUID id) {
        Entrega entrega = entregaRepository.findWithTareaAndEstudianteById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RECURSO, "id", id));

        validarPuedeConsultar(entrega);

        return entregaMapper.toResponse(entrega);
    }

    // El DOCENTE (dueno del curso de la tarea) o ADMIN crean la entrega.
    // El estudiante NO crea: solo modifica la suya mientras no este CALIFICADA.
    public EntregaResponse crear(CrearEntregaRequest request) {

        Tarea tarea = tareaRepository.findWithSemanaCursoDocenteById(request.tareaId())
                .orElseThrow(() -> new ResourceNotFoundException(RECURSO_TAREA, "id", request.tareaId()));

        Usuario estudiante = usuarioRepository.findWithRolesById(request.estudianteId())
                .orElseThrow(() -> new ResourceNotFoundException(RECURSO_USUARIO, "id", request.estudianteId()));

        validarAutorizacionDocente(tarea);
        validarEstudianteTieneRolEstudiante(estudiante);
        validarEstudianteMatriculado(tarea, estudiante);
        validarNoDuplicada(tarea.getId(), estudiante.getId());
        validarUrl(request.urlArchivo());

        Entrega entrega = entregaMapper.toEntity(request);
        entrega.setTarea(tarea);
        entrega.setEstudiante(estudiante);
        entrega.setEnviadoAt(LocalDateTime.now(AppTime.ZONA_NEGOCIO));
        entrega.setEstado(calcularEstado(tarea, entrega.getEnviadoAt()));

        Entrega guardada = entregaRepository.save(entrega);

        return entregaMapper.toResponse(
                entregaRepository.findWithTareaAndEstudianteById(guardada.getId()).orElseThrow()
        );
    }

    // El estudiante dueno sube/modifica su archivo. No permitido si ya esta CALIFICADA.
    public EntregaResponse actualizar(UUID id, ActualizarEntregaRequest request) {

        Entrega entrega = entregaRepository.findWithTareaAndEstudianteById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RECURSO, "id", id));

        validarPuedeModificar(entrega);
        validarNoCalificada(entrega);
        validarUrl(request.urlArchivo());

        entregaMapper.actualizarDesdeRequest(request, entrega);
        entrega.setEnviadoAt(LocalDateTime.now(AppTime.ZONA_NEGOCIO));
        entrega.setEstado(calcularEstado(entrega.getTarea(), entrega.getEnviadoAt()));

        return entregaMapper.toResponse(entrega);
    }

    public void eliminar(UUID id) {

        Entrega entrega = entregaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RECURSO, "id", id));

        entregaRepository.delete(entrega);
    }

    // ------------------------------------------------------------------------
    // VALIDACIONES
    // ------------------------------------------------------------------------

    // DOCENTE dueno del curso de la tarea, o ADMIN.
    private void validarAutorizacionDocente(Tarea tarea) {
        if (SecurityUtils.esAdmin()) {
            return;
        }
        UUID docenteId = tarea.getSemana().getUnidad().getCurso().getDocente().getId();
        if (!docenteId.equals(SecurityUtils.getUsuarioAutenticadoId())) {
            throw new AccessDeniedException("No tienes permisos para crear entregas en esta tarea");
        }
    }

    // Estudiante dueno, docente dueno del curso, o ADMIN.
    private void validarPuedeConsultar(Entrega entrega) {
        if (SecurityUtils.esAdmin()) {
            return;
        }
        UUID usuarioAutenticadoId = SecurityUtils.getUsuarioAutenticadoId();

        boolean esElEstudiante = entrega.getEstudiante().getId().equals(usuarioAutenticadoId);
        boolean esElDocente = entrega.getTarea().getSemana().getUnidad()
                .getCurso().getDocente().getId().equals(usuarioAutenticadoId);

        if (!esElEstudiante && !esElDocente) {
            throw new AccessDeniedException("No tienes permisos para consultar esta entrega");
        }
    }

    // Solo el estudiante dueno o ADMIN pueden modificar.
    private void validarPuedeModificar(Entrega entrega) {
        if (SecurityUtils.esAdmin()) {
            return;
        }
        if (!entrega.getEstudiante().getId().equals(SecurityUtils.getUsuarioAutenticadoId())) {
            throw new AccessDeniedException("No tienes permisos para modificar esta entrega");
        }
    }

    private void validarNoCalificada(Entrega entrega) {
        if (entrega.getEstado() == EstadoEntrega.CALIFICADA) {
            throw new BusinessException(
                    "ENTREGA_YA_CALIFICADA",
                    "No se puede modificar una entrega que ya fue calificada"
            );
        }
    }

    private void validarEstudianteTieneRolEstudiante(Usuario estudiante) {
        boolean esEstudiante = estudiante.getRoles().stream()
                .anyMatch(rol -> ROL_ESTUDIANTE.equals(rol.getNombre()));
        if (!esEstudiante) {
            throw new BusinessException(
                    "ESTUDIANTE_SIN_ROL",
                    "El usuario asignado no tiene el rol ESTUDIANTE"
            );
        }
    }

    private void validarEstudianteMatriculado(Tarea tarea, Usuario estudiante) {
        Curso curso = tarea.getSemana().getUnidad().getCurso();
        boolean matriculado = matriculaRepository.existsByCursoIdAndEstudianteId(
                curso.getId(),
                estudiante.getId()
        );
        if (!matriculado) {
            throw new BusinessException(
                    "ESTUDIANTE_NO_MATRICULADO",
                    "El estudiante no esta matriculado en el curso de la tarea"
            );
        }
    }

    private void validarNoDuplicada(UUID tareaId, UUID estudianteId) {
        if (entregaRepository.findByTareaIdAndEstudianteId(tareaId, estudianteId).isPresent()) {
            throw new BusinessException(
                    "ENTREGA_DUPLICADA",
                    "Ya existe una entrega de este estudiante para esta tarea"
            );
        }
    }

    private void validarUrl(String url) {
        if (!UrlValidator.esFormatoValido(url)) {
            throw new BusinessException(
                    "URL_INVALIDA",
                    "La URL del archivo no tiene un formato valido: " + url
            );
        }
    }

    // PENDIENTE si no hay deadline o si se entrego a tiempo. TARDE si paso la fecha limite.
    private EstadoEntrega calcularEstado(Tarea tarea, LocalDateTime enviadoAt) {
        if (tarea.getFechaLimite() == null) {
            return EstadoEntrega.PENDIENTE;
        }
        return enviadoAt.isAfter(tarea.getFechaLimite())
                ? EstadoEntrega.TARDE
                : EstadoEntrega.PENDIENTE;
    }
}