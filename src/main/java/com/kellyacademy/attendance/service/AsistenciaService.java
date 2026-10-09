package com.kellyacademy.attendance.service;

import com.kellyacademy.attendance.dto.request.ActualizarAsistenciaRequest;
import com.kellyacademy.attendance.dto.request.CrearAsistenciaRequest;
import com.kellyacademy.attendance.dto.response.AsistenciaResponse;
import com.kellyacademy.attendance.dto.response.AsistenciaResumenResponse;
import com.kellyacademy.attendance.entity.Asistencia;
import com.kellyacademy.attendance.enums.EstadoAsistencia;
import com.kellyacademy.attendance.mapper.AsistenciaMapper;
import com.kellyacademy.attendance.repository.AsistenciaRepository;
import com.kellyacademy.attendance.specification.AsistenciaSpecifications;
import com.kellyacademy.communication.enums.TipoNotificacion;
import com.kellyacademy.communication.service.NotificacionService;
import com.kellyacademy.course.entity.Clase;
import com.kellyacademy.course.repository.ClaseRepository;
import com.kellyacademy.enrollment.repository.MatriculaRepository;
import com.kellyacademy.enrollment.service.CalculoMatriculaService;
import com.kellyacademy.shared.config.AppTime;
import com.kellyacademy.shared.exception.BusinessException;
import com.kellyacademy.shared.exception.ResourceNotFoundException;
import com.kellyacademy.shared.util.SecurityUtils;
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
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AsistenciaService {

    private static final String ROL_ESTUDIANTE = "ESTUDIANTE";

    // Formato de fecha para mensajes de notificacion (sin helper compartido en el proyecto).
    private static final DateTimeFormatter FMT_FECHA_CLASE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AsistenciaRepository asistenciaRepository;
    private final ClaseRepository claseRepository;
    private final UsuarioRepository usuarioRepository;
    private final MatriculaRepository matriculaRepository;
    private final AsistenciaMapper asistenciaMapper;
    private final CalculoMatriculaService calculoMatriculaService;
    private final NotificacionService notificacionService;

    // -------- listar --------

    @Transactional(readOnly = true)
    public Page<AsistenciaResumenResponse> listar(UUID claseId, Pageable pageable) {
        UUID docenteFiltro = SecurityUtils.esAdmin() ? null : SecurityUtils.getUsuarioAutenticadoId();

        Specification<Asistencia> spec = Specification.allOf(
                AsistenciaSpecifications.porClase(claseId),
                AsistenciaSpecifications.porDocente(docenteFiltro)
        );

        return asistenciaRepository.findAll(spec, pageable)
                .map(asistenciaMapper::toResumenResponse);
    }

    @Transactional(readOnly = true)
    public Page<AsistenciaResumenResponse> listarMisAsistencias(Pageable pageable) {
        UUID estudianteId = SecurityUtils.getUsuarioAutenticadoId();

        Specification<Asistencia> spec = Specification.allOf(
                AsistenciaSpecifications.porEstudiante(estudianteId)
        );

        return asistenciaRepository.findAll(spec, pageable)
                .map(asistenciaMapper::toResumenResponse);
    }

    // -------- obtener --------

    @Transactional(readOnly = true)
    public AsistenciaResponse obtener(UUID id) {
        Asistencia asistencia = asistenciaRepository.findWithClaseAndEstudianteById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asistencia", "id", id));

        validarPuedeConsultar(asistencia);
        return asistenciaMapper.toResponse(asistencia);
    }

    // -------- crear --------

    public AsistenciaResponse crear(CrearAsistenciaRequest request) {
        Clase clase = claseRepository.findWithSemanaCursoDocenteById(request.claseId())
                .orElseThrow(() -> new ResourceNotFoundException("Clase", "id", request.claseId()));

        validarDocenteDueno(clase);

        if (clase.getFechaHora() == null) {
            throw new BusinessException("CLASE_SIN_FECHA", "La clase no tiene fecha asignada.");
        }
        if (clase.getFechaHora().isAfter(LocalDateTime.now(AppTime.ZONA_NEGOCIO))) {
            throw new BusinessException("CLASE_NO_IMPARTIDA", "La clase aun no se ha impartido.");
        }

        Usuario estudiante = usuarioRepository.findWithRolesById(request.estudianteId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", request.estudianteId()));

        boolean tieneRolEstudiante = estudiante.getRoles().stream()
                .anyMatch(r -> ROL_ESTUDIANTE.equals(r.getNombre()));
        if (!tieneRolEstudiante) {
            throw new BusinessException("ESTUDIANTE_SIN_ROL", "El usuario no tiene rol ESTUDIANTE.");
        }

        UUID cursoId = clase.getSemana().getUnidad().getCurso().getId();
        if (!matriculaRepository.existsByCursoIdAndEstudianteId(cursoId, estudiante.getId())) {
            throw new BusinessException(
                    "ESTUDIANTE_NO_MATRICULADO",
                    "El estudiante no esta matriculado en el curso de la clase."
            );
        }

        asistenciaRepository.findByClaseIdAndEstudianteId(clase.getId(), estudiante.getId())
                .ifPresent(a -> {
                    throw new BusinessException("ASISTENCIA_DUPLICADA", "Ya existe asistencia para esta clase y estudiante.");
                });

        Asistencia entity = asistenciaMapper.toEntity(request);
        entity.setClase(clase);
        entity.setEstudiante(estudiante);
        entity.setRegistradoAt(LocalDateTime.now(AppTime.ZONA_NEGOCIO));

        Asistencia guardada = asistenciaRepository.save(entity);

        matriculaRepository.findByCursoIdAndEstudianteId(cursoId, estudiante.getId())
                .ifPresent(m -> calculoMatriculaService.recalcular(m.getId()));

        // D1: notificar si el estado resultante es notificable (AUSENTE, TARDE, JUSTIFICADO).
        // Si es PRESENTE no se notifica.
        if (esNotificable(guardada.getEstado())) {
            notificarCambioAsistencia(guardada);
        }

        return asistenciaMapper.toResponse(guardada);
    }

    // -------- actualizar --------

    public AsistenciaResponse actualizar(UUID id, ActualizarAsistenciaRequest request) {
        Asistencia asistencia = asistenciaRepository.findWithClaseAndEstudianteById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asistencia", "id", id));

        validarDocenteDueno(asistencia.getClase());

        // D2: capturar el estado ANTERIOR antes de que el mapper lo sobreescriba.
        // CRITICO: si se captura despues de actualizarDesdeRequest, estadoAnterior == estadoNuevo.
        EstadoAsistencia estadoAnterior = asistencia.getEstado();

        asistenciaMapper.actualizarDesdeRequest(request, asistencia);

        UUID cursoId = asistencia.getClase().getSemana().getUnidad().getCurso().getId();
        UUID estudianteId = asistencia.getEstudiante().getId();
        matriculaRepository.findByCursoIdAndEstudianteId(cursoId, estudianteId)
                .ifPresent(m -> calculoMatriculaService.recalcular(m.getId()));

        // D2: notificar solo si el estado CAMBIO y el nuevo estado es notificable.
        // Si el estado no cambio (solo se edito observacion): no notificar.
        // Si el nuevo estado es PRESENTE: no notificar aunque antes fuera AUSENTE (D9).
        EstadoAsistencia estadoNuevo = asistencia.getEstado();
        if (!estadoNuevo.equals(estadoAnterior) && esNotificable(estadoNuevo)) {
            notificarCambioAsistencia(asistencia);
        }

        return asistenciaMapper.toResponse(asistencia);
    }

    // -------- eliminar --------

    public void eliminar(UUID id) {
        Asistencia asistencia = asistenciaRepository.findWithClaseAndEstudianteById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asistencia", "id", id));

        UUID cursoId = asistencia.getClase().getSemana().getUnidad().getCurso().getId();
        UUID estudianteId = asistencia.getEstudiante().getId();

        asistenciaRepository.delete(asistencia);

        matriculaRepository.findByCursoIdAndEstudianteId(cursoId, estudianteId)
                .ifPresent(m -> calculoMatriculaService.recalcular(m.getId()));
    }

    // -------- autorizacion --------

    private void validarDocenteDueno(Clase clase) {
        UUID docenteId = clase.getSemana().getUnidad().getCurso().getDocente().getId();
        SecurityUtils.validarDocenteDuenoOAdmin(docenteId, "No tiene permiso sobre esta asistencia.");
    }

    private void validarPuedeConsultar(Asistencia asistencia) {
        if (SecurityUtils.esAdmin()) {
            return;
        }
        UUID autenticadoId = SecurityUtils.getUsuarioAutenticadoId();

        // Docente dueno del curso de la clase.
        UUID docenteId = asistencia.getClase().getSemana().getUnidad().getCurso().getDocente().getId();
        if (docenteId.equals(autenticadoId)) {
            return;
        }

        // Estudiante dueno de la asistencia.
        if (asistencia.getEstudiante().getId().equals(autenticadoId)) {
            return;
        }

        throw new AccessDeniedException("No tiene permiso para consultar esta asistencia.");
    }

    // -------- helpers de notificacion --------

    /*
     Retorna true si el estado debe generar una notificacion al estudiante.
     D3: AUSENTE, TARDE y JUSTIFICADO son notificables. PRESENTE NO lo es.
     */
    private boolean esNotificable(EstadoAsistencia estado) {
        return estado == EstadoAsistencia.AUSENTE
                || estado == EstadoAsistencia.TARDE
                || estado == EstadoAsistencia.JUSTIFICADO;
    }

    /*
     Construye y persiste la notificacion para el estudiante destinatario.
     D4: tipo ASISTENCIA. D5: destinatario = estudiante (no docente).
     D6: link = "/api/asistencias/{id}".
     D7: mensaje varia segun estado; fecha formateada con AppTime.ZONA_NEGOCIO.
     D8: sincrona, misma transaccion.
     */
    private void notificarCambioAsistencia(Asistencia asistencia) {
        EstadoAsistencia estado = asistencia.getEstado();
        LocalDateTime fechaHora = asistencia.getClase().getFechaHora();

        String fechaFormateada = (fechaHora != null)
                ? fechaHora.atZone(AppTime.ZONA_NEGOCIO).format(FMT_FECHA_CLASE)
                : "fecha no asignada";

        String cuerpo = switch (estado) {
            case AUSENTE     -> "Registraron tu asistencia como AUSENTE para la clase del " + fechaFormateada + ".";
            case TARDE       -> "Registraron tu asistencia como TARDE para la clase del " + fechaFormateada + ".";
            case JUSTIFICADO -> "Tu falta fue JUSTIFICADA para la clase del " + fechaFormateada + ".";
            default          -> throw new IllegalStateException("Estado no notificable: " + estado);
        };

        notificacionService.crear(
                asistencia.getEstudiante().getId(),
                TipoNotificacion.ASISTENCIA,
                "Asistencia registrada",
                cuerpo,
                "/api/asistencias/" + asistencia.getId()
        );
    }
}
