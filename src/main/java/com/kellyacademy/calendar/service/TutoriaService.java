package com.kellyacademy.calendar.service;

import com.kellyacademy.calendar.dto.request.ActualizarTutoriaRequest;
import com.kellyacademy.calendar.dto.request.CrearTutoriaRequest;
import com.kellyacademy.calendar.dto.response.TutoriaResponse;
import com.kellyacademy.calendar.dto.response.TutoriaResumenResponse;
import com.kellyacademy.calendar.entity.Tutoria;
import com.kellyacademy.calendar.enums.EstadoTutoria;
import com.kellyacademy.calendar.mapper.TutoriaMapper;
import com.kellyacademy.calendar.repository.TutoriaRepository;
import com.kellyacademy.calendar.specification.TutoriaSpecifications;
import com.kellyacademy.course.entity.Curso;
import com.kellyacademy.course.repository.CursoRepository;
import com.kellyacademy.enrollment.entity.Matricula;
import com.kellyacademy.enrollment.enums.EstadoMatricula;
import com.kellyacademy.enrollment.repository.MatriculaRepository;
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
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class TutoriaService {

    private static final String ROL_ESTUDIANTE = "ESTUDIANTE";
    private static final String ROL_DOCENTE = "DOCENTE";

    private final TutoriaRepository tutoriaRepository;
    private final TutoriaMapper tutoriaMapper;
    private final UsuarioRepository usuarioRepository;
    private final CursoRepository cursoRepository;
    private final MatriculaRepository matriculaRepository;

    // -------- listar --------

    @Transactional(readOnly = true)
    public Page<TutoriaResumenResponse> listar(
            UUID cursoId,
            EstadoTutoria estado,
            LocalDateTime desde,
            LocalDateTime hasta,
            Pageable pageable
    ) {
        // No-admin: solo ve las tutorias donde es estudiante o docente.
        // Admin: ve todas.
        Specification<Tutoria> spec = Specification.allOf(
                SecurityUtils.esAdmin()
                        ? Specification.unrestricted()
                        : TutoriaSpecifications.involucraA(SecurityUtils.getUsuarioAutenticadoId()),
                TutoriaSpecifications.porCurso(cursoId),
                TutoriaSpecifications.porEstado(estado),
                TutoriaSpecifications.fechaDesde(desde),
                TutoriaSpecifications.fechaHasta(hasta)
        );

        return tutoriaRepository.findAll(spec, pageable)
                .map(tutoriaMapper::toResumenResponse);
    }

    // -------- obtener --------

    @Transactional(readOnly = true)
    public TutoriaResponse obtener(UUID id) {
        Tutoria tutoria = tutoriaRepository.findWithEstudianteDocenteCursoById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tutoria", "id", id));

        validarPuedeConsultar(tutoria);
        return tutoriaMapper.toResponse(tutoria);
    }

    // -------- crear --------

    public TutoriaResponse crear(CrearTutoriaRequest request) {
        // Deuda #46: el autenticado debe ser el estudiante, el docente o ADMIN.
        validarParticipanteOAdmin(request.estudianteId(), request.docenteId());

        // Deuda #45: fecha futura.
        validarFechaFutura(request.fecha());

        Usuario estudiante = usuarioRepository.findWithRolesById(request.estudianteId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", request.estudianteId()));
        validarTieneRol(estudiante, ROL_ESTUDIANTE, "ESTUDIANTE_SIN_ROL");


        Usuario docente = usuarioRepository.findWithRolesById(request.docenteId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", request.docenteId()));
        validarTieneRol(docente, ROL_DOCENTE, "DOCENTE_SIN_ROL");

        Curso curso = null;
        if (request.cursoId() != null) {
            curso = cursoRepository.findWithDocenteById(request.cursoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Curso", "id", request.cursoId()));

            validarPertenenciaAlCurso(curso, estudiante, docente);
        }

        Tutoria entity = tutoriaMapper.toEntity(request);
        entity.setEstudiante(estudiante);
        entity.setDocente(docente);
        entity.setCurso(curso);
        entity.setEstado(EstadoTutoria.PENDIENTE);
        entity.setNotas(null);

        Tutoria guardada = tutoriaRepository.save(entity);
        Tutoria cargada = tutoriaRepository.findWithEstudianteDocenteCursoById(guardada.getId())
                .orElseThrow(() -> new IllegalStateException("Tutoria recien creada no encontrada"));
        return tutoriaMapper.toResponse(cargada);
    }

    // -------- actualizar --------

    public TutoriaResponse actualizar(UUID id, ActualizarTutoriaRequest request) {
        Tutoria tutoria = tutoriaRepository.findWithEstudianteDocenteCursoById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tutoria", "id", id));

        // Deuda #46: autenticado debe ser estudiante, docente o ADMIN.
        validarPuedeModificar(tutoria);

        // Deuda #44: fecha y duracion solo editables si estado = PENDIENTE.
        // 'notas' es informacion, editable siempre.
        validarEdicionPermitida(tutoria, request);

        // Deuda #45: si se cambia la fecha, debe ser futura.
        if (!Objects.equals(tutoria.getFecha(), request.fecha())) {
            validarFechaFutura(request.fecha());
        }

        tutoriaMapper.actualizarDesdeRequest(request, tutoria);

        Tutoria guardada = tutoriaRepository.save(tutoria);
        Tutoria cargada = tutoriaRepository.findWithEstudianteDocenteCursoById(guardada.getId())
                .orElseThrow(() -> new IllegalStateException("Tutoria actualizada no encontrada"));
        return tutoriaMapper.toResponse(cargada);
    }

    // -------- eliminar --------

    public void eliminar(UUID id) {
        Tutoria tutoria = tutoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tutoria", "id", id));
        tutoriaRepository.delete(tutoria);
    }

    // ------------------------------------------------------------------------
    // VALIDACIONES DE NEGOCIO
    // ------------------------------------------------------------------------

    // Deuda #45.
    private void validarFechaFutura(LocalDateTime fecha) {
        if (!fecha.isAfter(LocalDateTime.now(AppTime.ZONA_NEGOCIO))) {
            throw new BusinessException(
                    "TUTORIA_FECHA_PASADA",
                    "La fecha de la tutoria debe ser posterior al momento actual."
            );
        }
    }

    // Deuda #46 — crear.
    private void validarParticipanteOAdmin(UUID estudianteId, UUID docenteId) {
        if (SecurityUtils.esAdmin()) {
            return;
        }
        UUID autenticadoId = SecurityUtils.getUsuarioAutenticadoId();
        if (!autenticadoId.equals(estudianteId) && !autenticadoId.equals(docenteId)) {
            throw new AccessDeniedException(
                    "Solo el estudiante, el docente o un ADMIN pueden crear esta tutoria."
            );
        }
    }

    // Deuda #46 — modificar.
    private void validarPuedeModificar(Tutoria tutoria) {
        if (SecurityUtils.esAdmin()) {
            return;
        }
        UUID autenticadoId = SecurityUtils.getUsuarioAutenticadoId();
        boolean esEstudiante = tutoria.getEstudiante().getId().equals(autenticadoId);
        boolean esDocente = tutoria.getDocente().getId().equals(autenticadoId);
        if (!esEstudiante && !esDocente) {
            throw new AccessDeniedException(
                    "Solo el estudiante, el docente o un ADMIN pueden modificar esta tutoria."
            );
        }
    }

    // Consultar: mismo criterio que modificar.
    private void validarPuedeConsultar(Tutoria tutoria) {
        validarPuedeModificar(tutoria);
    }

    // Deuda #44: si estado != PENDIENTE, no se puede cambiar fecha ni duracion.
    // 'notas' se puede cambiar siempre.
    private void validarEdicionPermitida(Tutoria tutoria, ActualizarTutoriaRequest request) {
        if (tutoria.getEstado() == EstadoTutoria.PENDIENTE) {
            return;
        }
        boolean cambiaFecha = !Objects.equals(tutoria.getFecha(), request.fecha());
        boolean cambiaDuracion = !Objects.equals(tutoria.getDuracionMinutos(), request.duracionMinutos());
        if (cambiaFecha || cambiaDuracion) {
            throw new BusinessException(
                    "TUTORIA_NO_EDITABLE",
                    "Solo se pueden modificar fecha y duracion de una tutoria en estado PENDIENTE."
            );
        }
    }

    private void validarTieneRol(Usuario usuario, String rolNombre, String codigo) {
        boolean tiene = usuario.getRoles().stream()
                .anyMatch(r -> rolNombre.equals(r.getNombre()));
        if (!tiene) {
            throw new BusinessException(codigo, "El usuario no tiene rol " + rolNombre + ".");
        }
    }

    // Si la tutoria se vincula a un curso:
    //   - el docente debe ser el docente dueno del curso
    //   - el estudiante debe estar matriculado ACTIVO en el curso
    private void validarPertenenciaAlCurso(Curso curso, Usuario estudiante, Usuario docente) {
        if (curso.getDocente() != null
                && !curso.getDocente().getId().equals(docente.getId())) {
            throw new BusinessException(
                    "TUTORIA_FUERA_DE_CURSO",
                    "El docente no es el docente dueno del curso referenciado."
            );
        }

        Matricula matricula = matriculaRepository
                .findByCursoIdAndEstudianteId(curso.getId(), estudiante.getId())
                .orElse(null);
        if (matricula == null || matricula.getEstado() != EstadoMatricula.ACTIVA) {
            throw new BusinessException(
                    "TUTORIA_FUERA_DE_CURSO",
                    "El estudiante no esta matriculado ACTIVO en el curso referenciado."
            );
        }
    }
}