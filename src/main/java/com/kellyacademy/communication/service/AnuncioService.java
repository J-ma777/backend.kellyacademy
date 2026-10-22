package com.kellyacademy.communication.service;

import com.kellyacademy.communication.dto.request.ActualizarAnuncioRequest;
import com.kellyacademy.communication.dto.request.CrearAnuncioRequest;
import com.kellyacademy.communication.dto.response.AnuncioResponse;
import com.kellyacademy.communication.dto.response.AnuncioResumenResponse;
import com.kellyacademy.communication.entity.Anuncio;
import com.kellyacademy.communication.enums.TipoNotificacion;
import com.kellyacademy.communication.mapper.AnuncioMapper;
import com.kellyacademy.communication.repository.AnuncioRepository;
import com.kellyacademy.communication.specification.AnuncioSpecifications;
import com.kellyacademy.course.entity.Curso;
import com.kellyacademy.course.repository.CursoRepository;
import com.kellyacademy.enrollment.entity.Matricula;
import com.kellyacademy.enrollment.enums.EstadoMatricula;
import com.kellyacademy.enrollment.repository.MatriculaRepository;
import com.kellyacademy.shared.exception.ResourceNotFoundException;
import com.kellyacademy.shared.util.SecurityUtils;
import com.kellyacademy.shared.util.HtmlSeguro;
import com.kellyacademy.shared.config.AppTime;
import com.kellyacademy.course.service.AccesoCursoService;
import com.kellyacademy.user.entity.Usuario;
import com.kellyacademy.user.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AnuncioService {

    private final AnuncioRepository anuncioRepository;
    private final AccesoCursoService acceso;
    private final com.kellyacademy.storage.ArchivoAccesoService archivos;
    private final CursoRepository cursoRepository;
    private final UsuarioRepository usuarioRepository;
    private final MatriculaRepository matriculaRepository;
    private final AnuncioMapper anuncioMapper;
    private final NotificacionService notificacionService;

    // -------- listar --------

    @Transactional(readOnly = true)
    public Page<AnuncioResumenResponse> listar(UUID cursoId, Boolean activo, Pageable pageable) {
        Specification<Anuncio> spec = Specification.allOf(
                permisoLectura(),
                AnuncioSpecifications.porCurso(cursoId),
                AnuncioSpecifications.porActivo(activo)
        );

        return anuncioRepository.findAll(spec, pageable)
                .map(anuncioMapper::toResumenResponse);
    }

    // -------- obtener --------

    @Transactional(readOnly = true)
    public AnuncioResponse obtener(UUID id) {
        Anuncio anuncio = anuncioRepository.findWithCursoAndAutorById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Anuncio", "id", id));
        acceso.validarLectura(anuncio.getCurso());
        if (!acceso.esGestor(anuncio.getCurso()) && (!Boolean.TRUE.equals(anuncio.getPublicado()) || !Boolean.TRUE.equals(anuncio.getActivo())))
            throw new org.springframework.security.access.AccessDeniedException("Anuncio no disponible");
        return anuncioMapper.toResponse(anuncio);
    }

    // -------- crear --------

    public AnuncioResponse crear(CrearAnuncioRequest request) {
        Curso curso = cursoRepository.findWithDocenteById(request.cursoId())
                .orElseThrow(() -> new ResourceNotFoundException("Curso", "id", request.cursoId()));

        validarDocenteDueno(curso);

        Usuario autor = usuarioRepository.findById(SecurityUtils.getUsuarioAutenticadoId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id",
                        SecurityUtils.getUsuarioAutenticadoId()));

        Anuncio entity = anuncioMapper.toEntity(request);
        entity.setCurso(curso);
        entity.setAutor(autor);
        entity.setActivo(true);
        entity.setCuerpo(HtmlSeguro.limpiar(request.cuerpo()));
        if(request.imagenUrl()!=null&&!request.imagenUrl().isBlank()){
            archivos.validarVinculo(request.imagenUrl(),curso.getId(),com.kellyacademy.storage.ArchivoService.Destino.ANUNCIO);
            entity.setImagenUrl(request.imagenUrl().trim());
        }
        entity.setPublicado(request.publicado() == null || request.publicado());
        entity.setFijado(Boolean.TRUE.equals(request.fijado()));
        if (entity.getPublicado()) entity.setPublicadoAt(java.time.LocalDateTime.now(AppTime.ZONA_NEGOCIO));

        Anuncio guardada = anuncioRepository.save(entity);

        // Notifica a estudiantes matriculados ACTIVOS en el curso, excluyendo al autor.
        if (Boolean.TRUE.equals(guardada.getPublicado())) notificarEstudiantesMatriculados(guardada);

        return anuncioMapper.toResponse(guardada);
    }

    // -------- actualizar --------

    public AnuncioResponse actualizar(UUID id, ActualizarAnuncioRequest request) {
        Anuncio anuncio = anuncioRepository.findWithCursoAndAutorById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Anuncio", "id", id));

        validarDocenteDueno(anuncio.getCurso());

        if(request.imagenUrl()!=null){
            archivos.validarVinculo(request.imagenUrl(),anuncio.getCurso().getId(),com.kellyacademy.storage.ArchivoService.Destino.ANUNCIO);
            anuncio.setImagenUrl(request.imagenUrl().isBlank()?null:request.imagenUrl().trim());
        }
        boolean estabaPublicado = Boolean.TRUE.equals(anuncio.getPublicado());
        anuncioMapper.actualizarDesdeRequest(request, anuncio);
        anuncio.setCuerpo(HtmlSeguro.limpiar(request.cuerpo()));
        if (request.fijado() != null) anuncio.setFijado(request.fijado());
        if (request.publicado() != null) anuncio.setPublicado(request.publicado());
        if (!estabaPublicado && Boolean.TRUE.equals(anuncio.getPublicado())) {
            anuncio.setPublicadoAt(java.time.LocalDateTime.now(AppTime.ZONA_NEGOCIO));
            if (Boolean.TRUE.equals(anuncio.getActivo())) notificarEstudiantesMatriculados(anuncio);
        }
        return anuncioMapper.toResponse(anuncio);
    }

    // -------- archivar (soft-delete) --------

    public AnuncioResponse archivar(UUID id) {
        Anuncio anuncio = anuncioRepository.findWithCursoAndAutorById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Anuncio", "id", id));

        validarDocenteDueno(anuncio.getCurso());

        // Idempotente: si ya esta archivado, no falla.
        if (!Boolean.FALSE.equals(anuncio.getActivo())) {
            anuncio.setActivo(false);
        }

        return anuncioMapper.toResponse(anuncio);
    }

    // -------- eliminar --------

    public void eliminar(UUID id) {
        Anuncio anuncio = anuncioRepository.findWithCursoAndAutorById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Anuncio", "id", id));

        validarDocenteDueno(anuncio.getCurso());
        anuncioRepository.delete(anuncio);
    }

    // -------- helpers --------

    private Specification<Anuncio> permisoLectura() {
        if (SecurityUtils.esAdmin()) return Specification.unrestricted();
        java.util.UUID usuarioId = SecurityUtils.getUsuarioAutenticadoId();
        return (root, query, cb) -> {
            var sub = query.subquery(java.util.UUID.class);
            var m = sub.from(Matricula.class);
            sub.select(m.get("id")).where(cb.equal(m.get("curso").get("id"), root.get("curso").get("id")),
                    cb.equal(m.get("estudiante").get("id"), usuarioId), cb.notEqual(m.get("estado"), EstadoMatricula.ABANDONADA));
            return cb.or(cb.equal(root.get("curso").get("docente").get("id"), usuarioId),
                    cb.and(cb.isTrue(root.get("publicado")), cb.isTrue(root.get("activo")), cb.exists(sub)));
        };
    }

    private void validarDocenteDueno(Curso curso) {
        UUID docenteId = curso.getDocente().getId();
        SecurityUtils.validarDocenteDuenoOAdmin(docenteId, "No tiene permiso sobre este anuncio.");
    }

    private void notificarEstudiantesMatriculados(Anuncio anuncio) {
        UUID cursoId = anuncio.getCurso().getId();
        UUID autorId = anuncio.getAutor().getId();

        List<Matricula> matriculas = matriculaRepository
                .findWithEstudianteByCursoIdAndEstado(cursoId, EstadoMatricula.ACTIVA);

        String completo = "Nuevo anuncio: " + anuncio.getTitulo();
        String titulo = completo.length() > 200 ? completo.substring(0, 197) + "..." : completo;
        String link = "/api/anuncios/" + anuncio.getId();

        List<NotificacionService.NuevaNotificacion> destinatarios = matriculas.stream()
                .map(matricula -> matricula.getEstudiante().getId())
                .filter(estudianteId -> !estudianteId.equals(autorId))
                .map(estudianteId -> new NotificacionService.NuevaNotificacion(
                        estudianteId,
                        TipoNotificacion.ANUNCIO,
                        titulo,
                        null,
                        link
                ))
                .toList();

        notificacionService.crearBatch(destinatarios);
    }
}