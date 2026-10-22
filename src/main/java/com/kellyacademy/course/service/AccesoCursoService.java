package com.kellyacademy.course.service;

import com.kellyacademy.course.entity.*;
import com.kellyacademy.course.repository.CursoRepository;
import com.kellyacademy.course.repository.SemanaRepository;
import com.kellyacademy.enrollment.enums.EstadoMatricula;
import com.kellyacademy.enrollment.repository.MatriculaRepository;
import com.kellyacademy.shared.exception.ResourceNotFoundException;
import com.kellyacademy.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccesoCursoService {
    private final CursoRepository cursoRepository;
    private final MatriculaRepository matriculaRepository;
    private final SemanaRepository semanaRepository;
    public void validarSemana(UUID id) {
        Semana semana = semanaRepository.findWithUnidadCursoDocenteById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Semana", "id", id));
        validarLectura(semana.getUnidad().getCurso());
    }

    public boolean esGestor(Curso curso) {
        return SecurityUtils.esAdmin() || curso.getDocente().getId().equals(SecurityUtils.getUsuarioAutenticadoId());
    }
    public Curso obtener(UUID id) {
        Curso curso = cursoRepository.findWithDocenteById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Curso", "id", id));
        validarLectura(curso);
        return curso;
    }
    public void validarGestor(Curso curso) {
        if (!esGestor(curso)) throw new AccessDeniedException("No puedes gestionar este curso");
    }
    public void validarLectura(Curso curso) {
        if (esGestor(curso)) return;
        var matricula = matriculaRepository.findByCursoIdAndEstudianteId(curso.getId(), SecurityUtils.getUsuarioAutenticadoId())
                .orElseThrow(() -> new AccessDeniedException("No perteneces a este curso"));
        if (matricula.getEstado() == EstadoMatricula.ABANDONADA)
            throw new AccessDeniedException("Tu matricula no esta activa");
    }
    public boolean puedeVer(ContenidoEntity contenido) {
        Curso curso = contenido.getSemana().getUnidad().getCurso();
        return esGestor(curso) || Boolean.TRUE.equals(contenido.getPublicado()) && Boolean.TRUE.equals(contenido.getVisible());
    }
    public void validarContenido(ContenidoEntity contenido) {
        validarLectura(contenido.getSemana().getUnidad().getCurso());
        if (!puedeVer(contenido)) throw new AccessDeniedException("Este contenido no esta disponible");
    }
    public void validarEstudianteActivo(Curso curso) {
        var matricula = matriculaRepository.findByCursoIdAndEstudianteId(curso.getId(), SecurityUtils.getUsuarioAutenticadoId())
                .orElseThrow(() -> new AccessDeniedException("No estas matriculado en este curso"));
        if (matricula.getEstado() != EstadoMatricula.ACTIVA && matricula.getEstado() != EstadoMatricula.RIESGO)
            throw new AccessDeniedException("La matricula no admite nuevas entregas");
    }
}
