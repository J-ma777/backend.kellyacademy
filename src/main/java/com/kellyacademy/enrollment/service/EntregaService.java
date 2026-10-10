package com.kellyacademy.enrollment.service;

import com.kellyacademy.course.entity.Curso;
import com.kellyacademy.course.entity.Tarea;
import com.kellyacademy.course.repository.TareaRepository;
import com.kellyacademy.enrollment.dto.request.ActualizarEntregaRequest;
import com.kellyacademy.enrollment.dto.request.CalificarEntregaRequest;
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

import java.math.BigDecimal;
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
    private final CalculoMatriculaService calculoMatriculaService;

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
    // urlArchivo es opcional: cubre el caso de trabajo en equipo donde un solo
    // integrante sube el archivo y el docente crea la entrega del resto sin URL.
    public EntregaResponse crear(CrearEntregaRequest request) {

        Tarea tarea = tareaRepository.findWithSemanaCursoDocenteById(request.tareaId())
                .orElseThrow(() -> new ResourceNotFoundException(RECURSO_TAREA, "id", request.tareaId()));

        Usuario estudiante = usuarioRepository.findWithRolesById(request.estudianteId())
                .orElseThrow(() -> new ResourceNotFoundException(RECURSO_USUARIO, "id", request.estudianteId()));

        validarAutorizacionDocente(tarea);
        validarEstudianteTieneRolEstudiante(estudiante);
        validarEstudianteMatriculado(tarea, estudiante);
        validarNoDuplicada(tarea.getId(), estudiante.getId());
        validarUrlOpcional(request.urlArchivo()); // antes: validarUrl

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

    // El DOCENTE dueno del curso de la tarea, o ADMIN, califica.
    // Re-calificacion permitida: sobrescribe nota, retroalimentacion,
    // calificadoAt y calificadoPor.
    // enviadoAt NO se toca (es la marca de envio, no de calificacion).
    // estado queda CALIFICADA (terminal).
    public EntregaResponse calificar(UUID id, CalificarEntregaRequest request) {

        Entrega entrega = entregaRepository.findWithTareaAndEstudianteById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RECURSO, "id", id));

        validarPuedeCalificar(entrega);
        validarNotaEnRango(request.nota(), entrega.getTarea());

        Usuario calificador = resolverUsuarioAutenticado();

        entrega.setNota(request.nota());
        entrega.setRetroalimentacion(request.retroalimentacion());
        entrega.setEstado(EstadoEntrega.CALIFICADA);
        entrega.setCalificadoAt(LocalDateTime.now(AppTime.ZONA_NEGOCIO));
        entrega.setCalificadoPor(calificador);

        UUID cursoId = entrega.getTarea().getSemana().getUnidad().getCurso().getId();
        UUID estudianteId = entrega.getEstudiante().getId();
        matriculaRepository.findByCursoIdAndEstudianteId(cursoId, estudianteId)
                .ifPresent(matricula -> calculoMatriculaService.recalcular(matricula.getId()));

        return entregaMapper.toResponse(entrega);
    }

    public void eliminar(UUID id) {

        Entrega entrega = entregaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RECURSO, "id", id));

        if (entrega.getEstado() == EstadoEntrega.CALIFICADA) {
            throw new BusinessException(
                    "ENTREGA_CALIFICADA_NO_ELIMINABLE",
                    "No se puede eliminar una entrega calificada"
            );
        }

        entregaRepository.delete(entrega);
    }

    // ------------------------------------------------------------------------
    // VALIDACIONES
    // ------------------------------------------------------------------------

    // Resuelve la entidad Usuario del autenticado. Necesario para setear
    // calificadoPor como @ManyToOne (no basta el UUID).
    private Usuario resolverUsuarioAutenticado() {
        UUID id = SecurityUtils.getUsuarioAutenticadoId();
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RECURSO_USUARIO, "id", id));
    }

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

    // DOCENTE dueno del curso de la tarea, o ADMIN.
    private void validarPuedeCalificar(Entrega entrega) {
        if (SecurityUtils.esAdmin()) {
            return;
        }
        UUID docenteId = entrega.getTarea().getSemana().getUnidad()
                .getCurso().getDocente().getId();
        if (!docenteId.equals(SecurityUtils.getUsuarioAutenticadoId())) {
            throw new AccessDeniedException("No tienes permisos para calificar esta entrega");
        }
    }

    // Rango: 0 <= nota <= tarea.puntajeMaximo.
    // Se compara con compareTo (BigDecimal.equals es scale-sensitive).
    private void validarNotaEnRango(BigDecimal nota, Tarea tarea) {
        if (nota == null) {
            throw new BusinessException("NOTA_INVALIDA", "La nota es obligatoria");
        }
        if (nota.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("NOTA_INVALIDA", "La nota no puede ser negativa");
        }
        BigDecimal puntajeMaximo = BigDecimal.valueOf(tarea.getPuntajeMaximo());
        if (nota.compareTo(puntajeMaximo) > 0) {
            throw new BusinessException(
                    "NOTA_EXCEDE_PUNTAJE_MAXIMO",
                    "La nota " + nota + " excede el puntaje maximo de la tarea (" + puntajeMaximo + ")"
            );
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

    // Validacion de URL obligatoria (usada por actualizar, donde el
    // estudiante siempre sube archivo).
    private void validarUrl(String url) {
        if (!UrlValidator.esFormatoValido(url)) {
            throw new BusinessException(
                    "URL_INVALIDA",
                    "La URL del archivo no tiene un formato valido: " + url
            );
        }
    }

    // Validacion de URL opcional (usada por crear). null es valido:
    // cubre el caso de trabajo en equipo donde el docente crea la entrega de
    // integrantes que no suben archivo propio.
    private void validarUrlOpcional(String url) {
        if (url == null) {
            return;
        }
        validarUrl(url);
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