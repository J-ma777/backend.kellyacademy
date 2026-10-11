package com.kellyacademy.attendance.service;

import com.kellyacademy.attendance.dto.request.ActualizarAsistenciaRequest;
import com.kellyacademy.attendance.dto.request.CrearAsistenciaRequest;
import com.kellyacademy.attendance.dto.request.CrearAsistenciasMasivasRequest;
import com.kellyacademy.attendance.dto.request.RegistroAsistenciaItemRequest;
import com.kellyacademy.attendance.dto.response.AsistenciaResponse;
import com.kellyacademy.attendance.dto.response.AsistenciaResumenResponse;
import com.kellyacademy.attendance.entity.Asistencia;
import com.kellyacademy.attendance.enums.EstadoAsistencia;
import com.kellyacademy.attendance.mapper.AsistenciaMapper;
import com.kellyacademy.attendance.repository.AsistenciaRepository;
import com.kellyacademy.attendance.specification.AsistenciaSpecifications;
import com.kellyacademy.communication.enums.TipoNotificacion;
import com.kellyacademy.communication.repository.NotificacionRepository;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AsistenciaService {

    private static final String ROL_ESTUDIANTE = "ESTUDIANTE";

    private static final DateTimeFormatter FMT_FECHA_CLASE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AsistenciaRepository asistenciaRepository;
    private final ClaseRepository claseRepository;
    private final UsuarioRepository usuarioRepository;
    private final MatriculaRepository matriculaRepository;
    private final AsistenciaMapper asistenciaMapper;
    private final CalculoMatriculaService calculoMatriculaService;
    private final NotificacionService notificacionService;
    private final NotificacionRepository notificacionRepository;

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

        if (esNotificable(guardada.getEstado())) {
            notificarCambioAsistenciaSiCorresponde(guardada);
        }

        return asistenciaMapper.toResponse(guardada);
    }

    // -------- crear masivo (#63) --------

    public List<AsistenciaResponse> crearMasivo(CrearAsistenciasMasivasRequest request) {

        Clase clase = claseRepository.findWithSemanaCursoDocenteById(request.claseId())
                .orElseThrow(() -> new ResourceNotFoundException("Clase", "id", request.claseId()));

        validarDocenteDueno(clase);
        validarClaseImpartida(clase);

        UUID cursoId = clase.getSemana().getUnidad().getCurso().getId();

        // 1. Detectar duplicados de estudianteId dentro del batch.
        Set<UUID> estudiantesVistos = new HashSet<>();
        Set<UUID> estudiantesDuplicados = new HashSet<>();
        for (RegistroAsistenciaItemRequest item : request.registros()) {
            if (!estudiantesVistos.add(item.estudianteId())) {
                estudiantesDuplicados.add(item.estudianteId());
            }
        }
        if (!estudiantesDuplicados.isEmpty()) {
            String mensaje = estudiantesDuplicados.stream()
                    .map(id -> id + ": el estudiante aparece mas de una vez en el batch.")
                    .collect(Collectors.joining(" | "));
            throw new BusinessException("ESTUDIANTE_DUPLICADO_EN_BATCH", mensaje);
        }

        // 2. Pre-validar cada registro. Acumula errores y los lanza juntos.
        Map<String, String> errores = new LinkedHashMap<>();
        Map<UUID, Usuario> estudiantesPorId = new LinkedHashMap<>();

        for (RegistroAsistenciaItemRequest item : request.registros()) {
            validarRegistroMasivo(item, clase, cursoId, estudiantesPorId, errores);
        }

        if (!errores.isEmpty()) {
            String detalle = errores.entrySet().stream()
                    .map(e -> e.getKey() + ": " + e.getValue())
                    .collect(Collectors.joining(" | "));
            throw new BusinessException(
                    "ASISTENCIAS_MASIVAS_CON_ERRORES",
                    "El batch contiene registros invalidos. Ninguna asistencia fue creada. Detalles: " + detalle
            );
        }

        // 3. Persistir el batch completo.
        LocalDateTime ahora = LocalDateTime.now(AppTime.ZONA_NEGOCIO);
        List<Asistencia> aGuardar = new ArrayList<>(request.registros().size());
        for (RegistroAsistenciaItemRequest item : request.registros()) {
            Asistencia entity = new Asistencia();
            entity.setClase(clase);
            entity.setEstudiante(estudiantesPorId.get(item.estudianteId()));
            entity.setEstado(item.estado());
            entity.setObservacion(item.observacion());
            entity.setRegistradoAt(ahora);
            aGuardar.add(entity);
        }
        List<Asistencia> guardadas = asistenciaRepository.saveAll(aGuardar);

        // 4. Recalcular matricula por estudiante.
        for (Asistencia asistencia : guardadas) {
            UUID estudianteId = asistencia.getEstudiante().getId();
            matriculaRepository.findByCursoIdAndEstudianteId(cursoId, estudianteId)
                    .ifPresent(m -> calculoMatriculaService.recalcular(m.getId()));
        }

        // 5. Notificar a los estudiantes con estado notificable.
        //    crearBatch es package-private en communication y no accesible aqui.
        for (Asistencia asistencia : guardadas) {
            if (esNotificable(asistencia.getEstado())) {
                notificarCambioAsistenciaSiCorresponde(asistencia);
            }
        }

        return guardadas.stream()
                .map(asistenciaMapper::toResponse)
                .toList();
    }

    // -------- actualizar --------

    public AsistenciaResponse actualizar(UUID id, ActualizarAsistenciaRequest request) {
        Asistencia asistencia = asistenciaRepository.findWithClaseAndEstudianteById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asistencia", "id", id));

        validarDocenteDueno(asistencia.getClase());

        EstadoAsistencia estadoAnterior = asistencia.getEstado();

        asistenciaMapper.actualizarDesdeRequest(request, asistencia);

        UUID cursoId = asistencia.getClase().getSemana().getUnidad().getCurso().getId();
        UUID estudianteId = asistencia.getEstudiante().getId();
        matriculaRepository.findByCursoIdAndEstudianteId(cursoId, estudianteId)
                .ifPresent(m -> calculoMatriculaService.recalcular(m.getId()));

        EstadoAsistencia estadoNuevo = asistencia.getEstado();
        if (!estadoNuevo.equals(estadoAnterior) && esNotificable(estadoNuevo)) {
            notificarCambioAsistenciaSiCorresponde(asistencia);
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

        UUID docenteId = asistencia.getClase().getSemana().getUnidad().getCurso().getDocente().getId();
        if (docenteId.equals(autenticadoId)) {
            return;
        }

        if (asistencia.getEstudiante().getId().equals(autenticadoId)) {
            return;
        }

        throw new AccessDeniedException("No tiene permiso para consultar esta asistencia.");
    }

    // -------- helpers crearMasivo --------

    private void validarClaseImpartida(Clase clase) {
        if (clase.getFechaHora() == null) {
            throw new BusinessException("CLASE_SIN_FECHA", "La clase no tiene fecha asignada.");
        }
        if (clase.getFechaHora().isAfter(LocalDateTime.now(AppTime.ZONA_NEGOCIO))) {
            throw new BusinessException("CLASE_NO_IMPARTIDA", "La clase aun no se ha impartido.");
        }
    }

    private void validarRegistroMasivo(RegistroAsistenciaItemRequest item,
                                       Clase clase,
                                       UUID cursoId,
                                       Map<UUID, Usuario> estudiantesPorId,
                                       Map<String, String> errores) {

        UUID estudianteId = item.estudianteId();

        Usuario estudiante = estudiantesPorId.get(estudianteId);
        if (estudiante == null) {
            var opt = usuarioRepository.findWithRolesById(estudianteId);
            if (opt.isEmpty()) {
                errores.put(estudianteId.toString(), "El estudiante no existe.");
                return;
            }
            estudiante = opt.get();
            estudiantesPorId.put(estudianteId, estudiante);
        }

        boolean tieneRolEstudiante = estudiante.getRoles().stream()
                .anyMatch(r -> ROL_ESTUDIANTE.equals(r.getNombre()));
        if (!tieneRolEstudiante) {
            errores.put(estudianteId.toString(), "El usuario no tiene rol ESTUDIANTE.");
            return;
        }

        if (!matriculaRepository.existsByCursoIdAndEstudianteId(cursoId, estudianteId)) {
            errores.put(estudianteId.toString(),
                    "El estudiante no esta matriculado en el curso de la clase.");
            return;
        }

        if (asistenciaRepository.findByClaseIdAndEstudianteId(clase.getId(), estudianteId).isPresent()) {
            errores.put(estudianteId.toString(),
                    "Ya existe asistencia para esta clase y estudiante.");
        }
    }

    // -------- helpers notificacion --------

    private boolean esNotificable(EstadoAsistencia estado) {
        return estado == EstadoAsistencia.AUSENTE
                || estado == EstadoAsistencia.TARDE
                || estado == EstadoAsistencia.JUSTIFICADO;
    }

    // Deduplicacion (#87): si ya existe una notificacion con el mismo link
    // (identifica la asistencia) y el mismo cuerpo (identifica el estado +
    // contexto), no se crea una nueva. Resuelve el caso AUSENTE -> TARDE ->
    // AUSENTE, donde la vuelta al estado original generaba una notificacion
    // duplicada respecto a la de la creacion.
    private void notificarCambioAsistenciaSiCorresponde(Asistencia asistencia) {
        String link = "/api/asistencias/" + asistencia.getId();
        String cuerpoNuevo = construirCuerpoNotificacion(asistencia);

        if (notificacionRepository.existsByLinkAndCuerpo(link, cuerpoNuevo)) {
            return;
        }

        notificacionService.crear(
                asistencia.getEstudiante().getId(),
                TipoNotificacion.ASISTENCIA,
                "Asistencia registrada",
                cuerpoNuevo,
                link
        );
    }

    private String construirCuerpoNotificacion(Asistencia asistencia) {
        EstadoAsistencia estado = asistencia.getEstado();
        LocalDateTime fechaHora = asistencia.getClase().getFechaHora();

        String fechaFormateada = (fechaHora != null)
                ? fechaHora.atZone(AppTime.ZONA_NEGOCIO).format(FMT_FECHA_CLASE)
                : "fecha no asignada";

        return switch (estado) {
            case AUSENTE     -> "Registraron tu asistencia como AUSENTE para la clase del " + fechaFormateada + ".";
            case TARDE       -> "Registraron tu asistencia como TARDE para la clase del " + fechaFormateada + ".";
            case JUSTIFICADO -> "Tu falta fue JUSTIFICADA para la clase del " + fechaFormateada + ".";
            default          -> throw new IllegalStateException("Estado no notificable: " + estado);
        };
    }
}