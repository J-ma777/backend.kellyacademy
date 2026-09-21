package com.kellyacademy.calendar.service;

import com.kellyacademy.calendar.dto.request.ActualizarEventoRequest;
import com.kellyacademy.calendar.dto.request.CrearEventoRequest;
import com.kellyacademy.calendar.dto.response.EventoResponse;
import com.kellyacademy.calendar.dto.response.EventoResumenResponse;
import com.kellyacademy.calendar.entity.Evento;
import com.kellyacademy.calendar.mapper.EventoMapper;
import com.kellyacademy.calendar.repository.EventoRepository;
import com.kellyacademy.calendar.specification.EventoSpecifications;
import com.kellyacademy.course.entity.Curso;
import com.kellyacademy.course.repository.CursoRepository;
import com.kellyacademy.enrollment.entity.Matricula;
import com.kellyacademy.enrollment.enums.EstadoMatricula;
import com.kellyacademy.enrollment.repository.MatriculaRepository;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class EventoService {

    private final EventoRepository eventoRepository;
    private final EventoMapper eventoMapper;
    private final UsuarioRepository usuarioRepository;
    private final CursoRepository cursoRepository;
    private final MatriculaRepository matriculaRepository;

    @Transactional(readOnly = true)
    public Page<EventoResumenResponse> listar(
            UUID usuarioId,
            UUID cursoId,
            com.kellyacademy.calendar.enums.TipoEvento tipo,
            LocalDateTime desde,
            LocalDateTime hasta,
            Pageable pageable
    ) {
        // Un usuario normal solo ve sus propios eventos. ADMIN puede filtrar por
        // cualquier usuario o ver todos si no envia usuarioId.
        UUID filtroUsuario = SecurityUtils.esAdmin() ? usuarioId : SecurityUtils.getUsuarioAutenticadoId();

        Specification<Evento> spec = Specification.allOf(
                EventoSpecifications.porUsuario(filtroUsuario),
                EventoSpecifications.porCurso(cursoId),
                EventoSpecifications.porTipo(tipo),
                EventoSpecifications.inicioDesde(desde),
                EventoSpecifications.inicioHasta(hasta)
        );

        return eventoRepository.findAll(spec, pageable)
                .map(eventoMapper::toResumenResponse);
    }

    @Transactional(readOnly = true)
    public EventoResponse obtener(UUID id) {
        Evento evento = eventoRepository.findWithUsuarioAndCursoById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evento", "id", id));

        validarPuedeConsultar(evento);
        return eventoMapper.toResponse(evento);
    }

    public EventoResponse crear(CrearEventoRequest request) {
        validarRangoTemporal(request.inicio(), request.fin());

        Usuario usuario = usuarioRepository.findById(SecurityUtils.getUsuarioAutenticadoId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario", "id", SecurityUtils.getUsuarioAutenticadoId()));

        Curso curso = null;
        if (request.cursoId() != null) {
            curso = cursoRepository.findWithDocenteById(request.cursoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Curso", "id", request.cursoId()));
            validarPertenenciaAlCurso(usuario.getId(), curso);
        }

        Evento evento = eventoMapper.toEntity(request);
        evento.setUsuario(usuario);
        evento.setCurso(curso);

        Evento guardado = eventoRepository.save(evento);
        // Recargamos con EntityGraph para que el mapper tenga usuario y curso cargados
        // dentro de la misma transaccion (open-in-view=false).
        Evento cargado = eventoRepository.findWithUsuarioAndCursoById(guardado.getId())
                .orElseThrow(() -> new IllegalStateException("Evento recien creado no encontrado"));
        return eventoMapper.toResponse(cargado);
    }

    public EventoResponse actualizar(UUID id, ActualizarEventoRequest request) {
        Evento evento = eventoRepository.findWithUsuarioAndCursoById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evento", "id", id));

        validarPuedeModificar(evento);
        validarRangoTemporal(request.inicio(), request.fin());

        // Inmutables: usuario, curso. El mapper los ignora por diseno.
        eventoMapper.actualizarDesdeRequest(request, evento);

        Evento guardado = eventoRepository.save(evento);
        Evento cargado = eventoRepository.findWithUsuarioAndCursoById(guardado.getId())
                .orElseThrow(() -> new IllegalStateException("Evento actualizado no encontrado"));
        return eventoMapper.toResponse(cargado);
    }

    public void eliminar(UUID id) {
        Evento evento = eventoRepository.findWithUsuarioAndCursoById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evento", "id", id));

        validarPuedeModificar(evento);
        eventoRepository.delete(evento);
    }

    // ------------------------------------------------------------------------
    // VALIDACIONES DE NEGOCIO
    // ------------------------------------------------------------------------

    // Deuda #38: fin > inicio cuando fin != null.
    private void validarRangoTemporal(LocalDateTime inicio, LocalDateTime fin) {
        if (fin != null && !fin.isAfter(inicio)) {
            throw new BusinessException(
                    "FECHAS_INVALIDAS",
                    "La fecha de fin debe ser posterior a la fecha de inicio"
            );
        }
    }

    // Deuda #42: al crear Evento con cursoId != null, el usuario debe pertenecer
    // al curso: docente dueno o estudiante con matricula ACTIVA.
    private void validarPertenenciaAlCurso(UUID usuarioId, Curso curso) {
        // Docente dueno
        if (curso.getDocente() != null && curso.getDocente().getId().equals(usuarioId)) {
            return;
        }

        // Estudiante con matricula ACTIVA
        Matricula matricula = matriculaRepository
                .findByCursoIdAndEstudianteId(curso.getId(), usuarioId)
                .orElse(null);

        if (matricula == null || matricula.getEstado() != EstadoMatricula.ACTIVA) {
            throw new BusinessException(
                    "USUARIO_NO_PERTENECE_AL_CURSO",
                    "Solo el docente dueno o estudiantes con matricula ACTIVA pueden vincular un evento al curso"
            );
        }
    }

    // Autorizacion: el dueno del evento o ADMIN.
    private void validarPuedeConsultar(Evento evento) {
        if (SecurityUtils.esAdmin()) {
            return;
        }
        if (!evento.getUsuario().getId().equals(SecurityUtils.getUsuarioAutenticadoId())) {
            throw new AccessDeniedException("No tienes permisos para consultar este evento");
        }
    }

    private void validarPuedeModificar(Evento evento) {
        if (SecurityUtils.esAdmin()) {
            return;
        }
        if (!evento.getUsuario().getId().equals(SecurityUtils.getUsuarioAutenticadoId())) {
            throw new AccessDeniedException("No tienes permisos para modificar este evento");
        }
    }
}