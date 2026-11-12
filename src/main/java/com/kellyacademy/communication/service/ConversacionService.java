package com.kellyacademy.communication.service;

import com.kellyacademy.communication.dto.request.CambiarAsuntoConversacionRequest;
import com.kellyacademy.communication.dto.request.CrearConversacionRequest;
import com.kellyacademy.communication.dto.response.ConversacionResponse;
import com.kellyacademy.communication.dto.response.ConversacionResumenResponse;
import com.kellyacademy.communication.entity.Conversacion;
import com.kellyacademy.communication.mapper.ConversacionMapper;
import com.kellyacademy.communication.repository.ConversacionRepository;
import com.kellyacademy.communication.repository.MensajeRepository;
import com.kellyacademy.communication.specification.ConversacionSpecifications;
import com.kellyacademy.course.entity.Curso;
import com.kellyacademy.course.repository.CursoRepository;
import com.kellyacademy.course.service.AccesoCursoService;
import com.kellyacademy.enrollment.enums.EstadoMatricula;
import com.kellyacademy.enrollment.repository.MatriculaRepository;
import com.kellyacademy.shared.exception.BusinessException;
import com.kellyacademy.shared.exception.ResourceNotFoundException;
import com.kellyacademy.shared.util.SecurityUtils;
import com.kellyacademy.user.entity.Usuario;
import com.kellyacademy.user.enums.EstadoUsuario;
import com.kellyacademy.user.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ConversacionService {

    private final ConversacionRepository conversacionRepository;
    private final MensajeRepository mensajeRepository;
    private final CursoRepository cursoRepository;
    private final UsuarioRepository usuarioRepository;
    private final MatriculaRepository matriculaRepository;
    private final ConversacionMapper conversacionMapper;
    private final AccesoCursoService accesoCursoService;

    // -------- listar --------

    @Transactional(readOnly = true)
    public Page<ConversacionResumenResponse> listar(UUID cursoId, Pageable pageable) {
        UUID usuarioFiltro = SecurityUtils.esAdmin() ? null : SecurityUtils.getUsuarioAutenticadoId();

        Specification<Conversacion> spec = Specification.allOf(
                ConversacionSpecifications.porParticipante(usuarioFiltro),
                ConversacionSpecifications.porCurso(cursoId)
        );

        UUID autenticadoId = SecurityUtils.getUsuarioAutenticadoId();

        return conversacionRepository.findAll(spec, pageable)
                .map(c -> {
                    long noLeidos = mensajeRepository
                            .countByConversacionIdAndRemitenteIdNotAndLeidoFalse(c.getId(), autenticadoId);
                    return conversacionMapper.toResumenResponse(c, noLeidos);
                });
    }

    public record Contacto(UUID id, String nombreCompleto, String avatarUrl) {}

    @Transactional(readOnly = true)
    public List<Contacto> contactos(UUID cursoId) {
        Curso curso = accesoCursoService.obtener(cursoId);
        if (!accesoCursoService.esGestor(curso)) accesoCursoService.validarEstudianteActivo(curso);
        UUID propio = SecurityUtils.getUsuarioAutenticadoId();
        var contactos = new LinkedHashMap<UUID, Contacto>();
        agregarContacto(contactos, curso.getDocente(), propio);
        matriculaRepository.findByCursoIdOrderByEstudianteApellidoAscEstudianteNombreAsc(cursoId).stream()
                .filter(m -> m.getEstado() == EstadoMatricula.ACTIVA || m.getEstado() == EstadoMatricula.RIESGO)
                .forEach(m -> agregarContacto(contactos, m.getEstudiante(), propio));
        return List.copyOf(contactos.values());
    }

    private void agregarContacto(LinkedHashMap<UUID, Contacto> contactos, Usuario u, UUID propio) {
        if (!u.getId().equals(propio) && u.getEstado() == EstadoUsuario.ACTIVO) {
            contactos.putIfAbsent(u.getId(), new Contacto(u.getId(), (u.getNombre() + " " + u.getApellido()).trim(), u.getAvatarUrl()));
        }
    }

    // -------- obtener --------

    @Transactional(readOnly = true)
    public ConversacionResponse obtener(UUID id) {
        Conversacion conversacion = conversacionRepository.findWithParticipantesById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conversacion", "id", id));

        validarParticipanteOAdmin(conversacion);

        UUID autenticadoId = SecurityUtils.getUsuarioAutenticadoId();
        long noLeidos = mensajeRepository
                .countByConversacionIdAndRemitenteIdNotAndLeidoFalse(id, autenticadoId);

        return conversacionMapper.toResponse(conversacion, noLeidos);
    }

    // -------- crear --------

    public ConversacionResponse crear(CrearConversacionRequest request) {
        UUID autenticadoId = SecurityUtils.getUsuarioAutenticadoId();
        UUID otroParticipanteId = request.otroParticipanteId();

        if (otroParticipanteId.equals(autenticadoId)) {
            throw new BusinessException("NO_AUTOCONVERSACION",
                    "No puede crear una conversacion consigo mismo.");
        }

        Usuario autenticado = usuarioRepository.findById(autenticadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", autenticadoId));

        Usuario otro = usuarioRepository.findById(otroParticipanteId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", otroParticipanteId));

        Curso curso = null;
        if (request.cursoId() != null) {
            curso = cursoRepository.findById(request.cursoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Curso", "id", request.cursoId()));
            validarParticipanteEnCurso(curso, autenticado);
            validarParticipanteEnCurso(curso, otro);
        }

        // Normalizar orden por UUID para evitar duplicados (A,B) vs (B,A).
        // Deuda #28: mitigacion en servicio; solucion robusta es indice funcional.
        Usuario p1;
        Usuario p2;
        if (autenticado.getId().compareTo(otro.getId()) < 0) {
            p1 = autenticado;
            p2 = otro;
        } else {
            p1 = otro;
            p2 = autenticado;
        }

        Optional<Conversacion> existente = curso == null
                ? conversacionRepository.findByCursoIsNullAndParticipante1IdAndParticipante2Id(p1.getId(), p2.getId())
                : conversacionRepository.findByCursoIdAndParticipante1IdAndParticipante2Id(curso.getId(), p1.getId(), p2.getId());

        if (existente.isPresent()) {
            Conversacion c = existente.get();
            long noLeidos = mensajeRepository
                    .countByConversacionIdAndRemitenteIdNotAndLeidoFalse(c.getId(), autenticadoId);
            return conversacionMapper.toResponse(c, noLeidos);
        }

        Conversacion entity = conversacionMapper.toEntity(request);
        entity.setCurso(curso);
        entity.setParticipante1(p1);
        entity.setParticipante2(p2);
        entity.setUltimoMensajeAt(null);

        Conversacion guardada = conversacionRepository.save(entity);
        return conversacionMapper.toResponse(guardada, 0L);
    }

    // -------- cambiarAsunto --------

    // Cualquier participante (o ADMIN) puede renombrar la conversacion.
    // Rechaza no-op con ASUNTO_SIN_CAMBIOS, alineado con el patron de estados.
    public ConversacionResponse cambiarAsunto(UUID id, CambiarAsuntoConversacionRequest request) {

        Conversacion conversacion = conversacionRepository.findWithParticipantesById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conversacion", "id", id));

        validarParticipanteOAdmin(conversacion);

        if (request.asunto().equals(conversacion.getAsunto())) {
            throw new BusinessException(
                    "ASUNTO_SIN_CAMBIOS",
                    "El asunto nuevo es igual al actual"
            );
        }

        conversacion.setAsunto(request.asunto());

        UUID autenticadoId = SecurityUtils.getUsuarioAutenticadoId();
        long noLeidos = mensajeRepository
                .countByConversacionIdAndRemitenteIdNotAndLeidoFalse(id, autenticadoId);

        return conversacionMapper.toResponse(conversacion, noLeidos);
    }

    // -------- eliminar --------

    public void eliminar(UUID id) {
        Conversacion conversacion = conversacionRepository.findWithParticipantesById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conversacion", "id", id));

        validarParticipanteOAdmin(conversacion);
        conversacionRepository.delete(conversacion);
    }

    // -------- helpers --------

    private void validarParticipanteOAdmin(Conversacion conversacion) {
        if (SecurityUtils.esAdmin()) {
            return;
        }
        UUID autenticadoId = SecurityUtils.getUsuarioAutenticadoId();
        boolean esParticipante = conversacion.getParticipante1().getId().equals(autenticadoId)
                || conversacion.getParticipante2().getId().equals(autenticadoId);
        if (!esParticipante) {
            throw new AccessDeniedException("No tiene permiso para acceder a esta conversacion.");
        }
    }

    private void validarParticipanteEnCurso(Curso curso, Usuario usuario) {
        // Docente dueno del curso o estudiante con matricula ACTIVA.
        UUID docenteId = curso.getDocente().getId();
        if (docenteId.equals(usuario.getId())) {
            return;
        }
        boolean matriculadoActivo = matriculaRepository
                .findByCursoIdAndEstudianteId(curso.getId(), usuario.getId())
                .map(m -> m.getEstado() == EstadoMatricula.ACTIVA || m.getEstado() == EstadoMatricula.RIESGO)
                .orElse(false);
        if (!matriculadoActivo) {
            throw new BusinessException("PARTICIPANTE_NO_EN_CURSO",
                    "El usuario " + usuario.getId() + " no pertenece al curso " + curso.getId() + ".");
        }
    }
}