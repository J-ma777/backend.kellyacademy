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
    private final CursoRepository cursoRepository;
    private final UsuarioRepository usuarioRepository;
    private final MatriculaRepository matriculaRepository;
    private final AnuncioMapper anuncioMapper;
    private final NotificacionService notificacionService;

    // -------- listar --------

    @Transactional(readOnly = true)
    public Page<AnuncioResumenResponse> listar(UUID cursoId, Boolean activo, Pageable pageable) {
        Specification<Anuncio> spec = Specification.allOf(
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

        Anuncio guardada = anuncioRepository.save(entity);

        // Notifica a estudiantes matriculados ACTIVOS en el curso, excluyendo al autor.
        notificarEstudiantesMatriculados(guardada);

        return anuncioMapper.toResponse(guardada);
    }

    // -------- actualizar --------

    public AnuncioResponse actualizar(UUID id, ActualizarAnuncioRequest request) {
        Anuncio anuncio = anuncioRepository.findWithCursoAndAutorById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Anuncio", "id", id));

        validarDocenteDueno(anuncio.getCurso());

        anuncioMapper.actualizarDesdeRequest(request, anuncio);
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

    private void validarDocenteDueno(Curso curso) {
        UUID docenteId = curso.getDocente().getId();
        SecurityUtils.validarDocenteDuenoOAdmin(docenteId, "No tiene permiso sobre este anuncio.");
    }

    private void notificarEstudiantesMatriculados(Anuncio anuncio) {
        UUID cursoId = anuncio.getCurso().getId();
        UUID autorId = anuncio.getAutor().getId();

        List<Matricula> matriculas = matriculaRepository
                .findWithEstudianteByCursoIdAndEstado(cursoId, EstadoMatricula.ACTIVA);

        String titulo = "Nuevo anuncio: " + anuncio.getTitulo();
        String link = "/api/anuncios/" + anuncio.getId();

        for (Matricula matricula : matriculas) {
            UUID estudianteId = matricula.getEstudiante().getId();
            if (estudianteId.equals(autorId)) {
                continue; // No auto-notificarse.
            }
            notificacionService.crear(
                    estudianteId,
                    TipoNotificacion.ANUNCIO,
                    titulo,
                    null,
                    link
            );
        }
    }
}