package com.kellyacademy.attendance.service;

import com.kellyacademy.attendance.dto.request.ActualizarAsistenciaRequest;
import com.kellyacademy.attendance.dto.request.CrearAsistenciaRequest;
import com.kellyacademy.attendance.dto.response.AsistenciaResponse;
import com.kellyacademy.attendance.dto.response.AsistenciaResumenResponse;
import com.kellyacademy.attendance.entity.Asistencia;
import com.kellyacademy.attendance.mapper.AsistenciaMapper;
import com.kellyacademy.attendance.repository.AsistenciaRepository;
import com.kellyacademy.attendance.specification.AsistenciaSpecifications;
import com.kellyacademy.course.entity.Clase;
import com.kellyacademy.course.repository.ClaseRepository;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AsistenciaService {

    private static final String ROL_ESTUDIANTE = "ESTUDIANTE";

    private final AsistenciaRepository asistenciaRepository;
    private final ClaseRepository claseRepository;
    private final UsuarioRepository usuarioRepository;
    private final MatriculaRepository matriculaRepository;
    private final AsistenciaMapper asistenciaMapper;

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
        return asistenciaMapper.toResponse(guardada);
    }

    // -------- actualizar --------

    public AsistenciaResponse actualizar(UUID id, ActualizarAsistenciaRequest request) {
        Asistencia asistencia = asistenciaRepository.findWithClaseAndEstudianteById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asistencia", "id", id));

        validarDocenteDueno(asistencia.getClase());

        asistenciaMapper.actualizarDesdeRequest(request, asistencia);
        return asistenciaMapper.toResponse(asistencia);
    }

    // -------- eliminar --------

    public void eliminar(UUID id) {
        Asistencia asistencia = asistenciaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asistencia", "id", id));
        asistenciaRepository.delete(asistencia);
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
}