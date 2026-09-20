package com.kellyacademy.course.service;

import com.kellyacademy.course.dto.request.ActualizarCursoRequest;
import com.kellyacademy.course.dto.request.CrearCursoRequest;
import com.kellyacademy.course.dto.response.CursoResponse;
import com.kellyacademy.course.dto.response.CursoResumenResponse;
import com.kellyacademy.course.entity.Curso;
import com.kellyacademy.course.enums.EstadoCurso;
import com.kellyacademy.course.enums.NivelCefr;
import com.kellyacademy.course.mapper.CursoMapper;
import com.kellyacademy.course.repository.CursoRepository;
import com.kellyacademy.course.specification.CursoSpecifications;
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

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.data.jpa.domain.Specification.allOf;

@Service
@RequiredArgsConstructor
@Transactional
public class CursoService {

    private static final String ROL_DOCENTE = "DOCENTE";

    private final CursoRepository cursoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CursoMapper cursoMapper;
    private static final String RECURSO = "Curso";
    private static final String RECURSO_USUARIO = "Usuario";

    @Transactional(readOnly = true)
    public Page<CursoResumenResponse> listar(
            EstadoCurso estado,
            NivelCefr nivelCefr,
            UUID docenteId,
            String titulo,
            Pageable pageable
    ) {
        // El @EntityGraph declarado en CursoRepository.findAll(Spec, Pageable) trae el
        // docente en la misma query. Sin el, el mapper dispararia N+1 al leer
        // curso.getDocente().getId() por cada fila (docente es LAZY, open-in-view=false).
        Specification<Curso> spec = allOf(
                CursoSpecifications.porEstado(estado),
                CursoSpecifications.porNivelCefr(nivelCefr),
                CursoSpecifications.porDocenteId(docenteId),
                CursoSpecifications.tituloContiene(titulo)
        );

        return cursoRepository.findAll(spec, pageable)
                .map(cursoMapper::toResumenResponse);
    }

    @Transactional(readOnly = true)
    public CursoResponse obtener(UUID id) {
        Curso curso = cursoRepository.findWithDocenteById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RECURSO, "id", id));

        return cursoMapper.toResponse(curso);
    }

    public CursoResponse crear(CrearCursoRequest request) {

        Usuario docente = usuarioRepository.findWithRolesById(request.docenteId())
                .orElseThrow(() -> new ResourceNotFoundException(RECURSO_USUARIO, "id", request.docenteId()));

        validarDocenteTieneRolDocente(docente);
        validarFechas(request.fechaInicio(), request.fechaFin());

        Curso curso = cursoMapper.toEntity(request);
        curso.setDocente(docente);
        curso.setEstado(EstadoCurso.BORRADOR);

        Curso guardado = cursoRepository.save(curso);

        // Recargamos con @EntityGraph para que el mapper lea el docente sin LazyInit.
        return cursoMapper.toResponse(
                cursoRepository.findWithDocenteById(guardado.getId()).orElseThrow()
        );
    }

    public CursoResponse actualizar(UUID id, ActualizarCursoRequest request) {

        Curso curso = cursoRepository.findWithDocenteById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RECURSO, "id", id));

        validarPropietarioOAdmin(curso);
        validarFechas(request.fechaInicio(), request.fechaFin());

        cursoMapper.actualizarDesdeRequest(request, curso);

        return cursoMapper.toResponse(curso);
    }

    public void eliminar(UUID id) {

        Curso curso = cursoRepository.findWithDocenteById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RECURSO, "id", id));

        validarPropietarioOAdmin(curso);

        cursoRepository.delete(curso);
    }

    // Autorizacion fina: solo el docente dueno del curso o un ADMIN pueden modificar/eliminar.
    private void validarPropietarioOAdmin(Curso curso) {
        if (SecurityUtils.esAdmin()) {
            return;
        }
        UUID docenteId = curso.getDocente().getId();
        if (!docenteId.equals(SecurityUtils.getUsuarioAutenticadoId())) {
            throw new AccessDeniedException("No tienes permisos para modificar este curso");
        }
    }

    private void validarDocenteTieneRolDocente(Usuario docente) {
        boolean esDocente = docente.getRoles().stream()
                .anyMatch(rol -> ROL_DOCENTE.equals(rol.getNombre()));
        if (!esDocente) {
            throw new BusinessException(
                    "DOCENTE_SIN_ROL",
                    "El usuario asignado no tiene el rol DOCENTE"
            );
        }
    }

    private void validarFechas(LocalDate fechaInicio, LocalDate fechaFin) {
        if (fechaInicio != null && fechaFin != null && fechaFin.isBefore(fechaInicio)) {
            throw new BusinessException(
                    "FECHAS_INVALIDAS",
                    "La fecha de fin no puede ser anterior a la fecha de inicio"
            );
        }
    }
}