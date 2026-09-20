package com.kellyacademy.enrollment.service;

import com.kellyacademy.course.entity.Curso;
import com.kellyacademy.course.enums.EstadoCurso;
import com.kellyacademy.course.repository.CursoRepository;
import com.kellyacademy.enrollment.dto.request.CrearMatriculaRequest;
import com.kellyacademy.enrollment.dto.response.MatriculaResponse;
import com.kellyacademy.enrollment.dto.response.MatriculaResumenResponse;
import com.kellyacademy.enrollment.entity.Matricula;
import com.kellyacademy.enrollment.enums.EstadoMatricula;
import com.kellyacademy.enrollment.mapper.MatriculaMapper;
import com.kellyacademy.enrollment.repository.MatriculaRepository;
import com.kellyacademy.enrollment.specification.MatriculaSpecifications;
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
import java.util.UUID;

import static org.springframework.data.jpa.domain.Specification.allOf;

@Service
@RequiredArgsConstructor
@Transactional
public class MatriculaService {

    private static final String RECURSO = "Matricula";
    private static final String RECURSO_CURSO = "Curso";
    private static final String RECURSO_USUARIO = "Usuario";
    private static final String ROL_ESTUDIANTE = "ESTUDIANTE";

    private final MatriculaRepository matriculaRepository;
    private final CursoRepository cursoRepository;
    private final UsuarioRepository usuarioRepository;
    private final MatriculaMapper matriculaMapper;

    @Transactional(readOnly = true)
    public Page<MatriculaResumenResponse> listar(
            UUID cursoId,
            UUID estudianteId,
            EstadoMatricula estado,
            Pageable pageable
    ) {
        Specification<Matricula> spec = allOf(
                MatriculaSpecifications.porCursoId(cursoId),
                MatriculaSpecifications.porEstudianteId(estudianteId),
                MatriculaSpecifications.porEstado(estado)
        );

        return matriculaRepository.findAll(spec, pageable)
                .map(matriculaMapper::toResumenResponse);
    }

    @Transactional(readOnly = true)
    public MatriculaResponse obtener(UUID id) {
        Matricula matricula = matriculaRepository.findWithCursoAndEstudianteById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RECURSO, "id", id));

        validarPuedeConsultar(matricula);

        return matriculaMapper.toResponse(matricula);
    }

    public MatriculaResponse crear(CrearMatriculaRequest request) {

        Curso curso = cursoRepository.findWithDocenteById(request.cursoId())
                .orElseThrow(() -> new ResourceNotFoundException(RECURSO_CURSO, "id", request.cursoId()));

        Usuario estudiante = usuarioRepository.findWithRolesById(request.estudianteId())
                .orElseThrow(() -> new ResourceNotFoundException(RECURSO_USUARIO, "id", request.estudianteId()));

        validarEstudianteTieneRolEstudiante(estudiante);
        validarCursoActivo(curso);
        validarCupoDisponible(curso);
        validarNoDuplicada(curso.getId(), estudiante.getId());

        Matricula matricula = matriculaMapper.toEntity(request);
        matricula.setCurso(curso);
        matricula.setEstudiante(estudiante);
        matricula.setEstado(EstadoMatricula.ACTIVA);
        matricula.setMatriculadoAt(LocalDateTime.now(AppTime.ZONA_NEGOCIO));

        Matricula guardada = matriculaRepository.save(matricula);

        // Recargamos con @EntityGraph para que el mapper no dispare LazyInit.
        return matriculaMapper.toResponse(
                matriculaRepository.findWithCursoAndEstudianteById(guardada.getId()).orElseThrow()
        );
    }

    public void eliminar(UUID id) {

        Matricula matricula = matriculaRepository.findWithCursoAndEstudianteById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RECURSO, "id", id));

        matriculaRepository.delete(matricula);
    }

    // Un estudiante solo puede consultar sus propias matriculas. Un docente solo
    // las de sus cursos. Un ADMIN ve todas.
    private void validarPuedeConsultar(Matricula matricula) {
        if (SecurityUtils.esAdmin()) {
            return;
        }
        UUID usuarioAutenticadoId = SecurityUtils.getUsuarioAutenticadoId();

        boolean esElEstudiante = matricula.getEstudiante().getId().equals(usuarioAutenticadoId);
        boolean esElDocente = matricula.getCurso().getDocente().getId().equals(usuarioAutenticadoId);

        if (!esElEstudiante && !esElDocente) {
            throw new AccessDeniedException("No tienes permisos para consultar esta matricula");
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

    // Solo se puede matricular en cursos ACTIVOS. Un curso en BORRADOR aun no
    // esta publicado; FINALIZADO y ARCHIVADO no admiten nuevas matriculas.
    private void validarCursoActivo(Curso curso) {
        if (curso.getEstado() != EstadoCurso.ACTIVO) {
            throw new BusinessException(
                    "CURSO_NO_ACTIVO",
                    "El curso no esta activo. Estado actual: " + curso.getEstado()
            );
        }
    }

    // Cupo: matriculas ACTIVAS del curso < capacidadMaxima.
    private void validarCupoDisponible(Curso curso) {
        long matriculasActivas = matriculaRepository.countByCursoIdAndEstado(
                curso.getId(),
                EstadoMatricula.ACTIVA
        );
        if (matriculasActivas >= curso.getCapacidadMaxima()) {
            throw new BusinessException(
                    "CURSO_SIN_CUPO",
                    "El curso alcanzo su capacidad maxima de " + curso.getCapacidadMaxima() + " estudiantes"
            );
        }
    }

    private void validarNoDuplicada(UUID cursoId, UUID estudianteId) {
        if (matriculaRepository.existsByCursoIdAndEstudianteId(cursoId, estudianteId)) {
            throw new BusinessException(
                    "MATRICULA_DUPLICADA",
                    "El estudiante ya esta matriculado en este curso"
            );
        }
    }
}