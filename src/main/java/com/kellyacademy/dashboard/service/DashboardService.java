package com.kellyacademy.dashboard.service;

import com.kellyacademy.course.entity.Clase;
import com.kellyacademy.course.entity.Curso;
import com.kellyacademy.course.entity.Tarea;
import com.kellyacademy.course.repository.ClaseRepository;
import com.kellyacademy.course.repository.CursoRepository;
import com.kellyacademy.course.repository.TareaRepository;
import com.kellyacademy.dashboard.dto.DashboardResumenResponse;
import com.kellyacademy.dashboard.dto.DashboardResumenResponse.ClaseHoyDto;
import com.kellyacademy.dashboard.dto.DashboardResumenResponse.MetricasDto;
import com.kellyacademy.dashboard.dto.DashboardResumenResponse.TareaPendienteDto;
import com.kellyacademy.enrollment.entity.Entrega;
import com.kellyacademy.enrollment.entity.Matricula;
import com.kellyacademy.enrollment.enums.EstadoMatricula;
import com.kellyacademy.enrollment.repository.EntregaRepository;
import com.kellyacademy.enrollment.repository.MatriculaRepository;
import com.kellyacademy.security.user.CustomUserDetails;
import com.kellyacademy.shared.util.SecurityUtils;
import com.kellyacademy.user.entity.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Set;
import java.util.stream.Collectors;
import com.kellyacademy.shared.config.AppTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final MatriculaRepository matriculaRepository;
    private final CursoRepository cursoRepository;
    private final EntregaRepository entregaRepository;
    private final TareaRepository tareaRepository;
    private final com.kellyacademy.calendar.service.AgendaService agenda;

    public DashboardResumenResponse obtenerResumen() {
        CustomUserDetails userDetails = SecurityUtils.getUsuarioAutenticado();
        Usuario usuario = userDetails.getUsuario();
        UUID usuarioId = usuario.getId();
        boolean esDocente = userDetails.getAuthorities().stream()
                .anyMatch(a -> "ROLE_DOCENTE".equals(a.getAuthority()));
        boolean esAdmin = SecurityUtils.esAdmin();

        String rol = esAdmin ? "ADMINISTRADOR" : (esDocente ? "DOCENTE" : "ESTUDIANTE");
        String nombre = usuario.getNombre() + (usuario.getApellido() != null ? " " + usuario.getApellido() : "");

        if (esDocente || esAdmin) {
            return construirResumenDocente(usuarioId, rol, nombre.trim());
        } else {
            return construirResumenEstudiante(usuarioId, rol, nombre.trim());
        }
    }

    private DashboardResumenResponse construirResumenEstudiante(UUID estudianteId, String rol, String nombre) {
        List<Matricula> matriculas = matriculaRepository.findByEstudianteId(estudianteId);
        List<Matricula> matriculasActivas = matriculas.stream()
                .filter(m -> m.getEstado() == EstadoMatricula.ACTIVA || m.getEstado() == EstadoMatricula.RIESGO)
                .toList();

        long totalCursos = matriculas.stream().filter(m -> m.getEstado() != EstadoMatricula.ABANDONADA).map(m -> m.getCurso().getId()).distinct().count();

        // Calculo de promedio
        BigDecimal sumaNotas = BigDecimal.ZERO;
        int notasContadas = 0;
        for (Matricula m : matriculasActivas) {
            if (m.getNotaFinal() != null) {
                sumaNotas = sumaNotas.add(m.getNotaFinal());
                notasContadas++;
            }
        }
        BigDecimal promedio = notasContadas > 0
                ? sumaNotas.divide(BigDecimal.valueOf(notasContadas), 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Calculo de asistencia
        BigDecimal sumaAsistencia = BigDecimal.ZERO;
        int asistenciaContada = 0;
        for (Matricula m : matriculasActivas) {
            if (m.getAsistenciaPorcentaje() != null) {
                sumaAsistencia = sumaAsistencia.add(m.getAsistenciaPorcentaje());
                asistenciaContada++;
            }
        }
        BigDecimal asistencia = asistenciaContada > 0
                ? sumaAsistencia.divide(BigDecimal.valueOf(asistenciaContada), 0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<Entrega> entregas = entregaRepository.findByEstudianteId(estudianteId);
        long tareasEntregadas = entregas.size();

        MetricasDto metricas = new MetricasDto(
                totalCursos,
                promedio,
                tareasEntregadas,
                asistencia,
                0.0
        );

        // Clases de hoy o proximas
        Set<UUID> cursoIds = matriculas.stream().filter(m -> m.getEstado() != EstadoMatricula.ABANDONADA).map(m -> m.getCurso().getId()).collect(Collectors.toSet());
        List<ClaseHoyDto> clasesHoy = obtenerClasesHoyOProximas();

        // Tareas pendientes
        Set<UUID> entregadas = entregas.stream().map(e -> e.getTarea().getId()).collect(Collectors.toSet());
        List<TareaPendienteDto> tareasPendientes = obtenerTareasPendientes(cursoIds, false, entregadas);

        return new DashboardResumenResponse(metricas, clasesHoy, tareasPendientes, rol, nombre);
    }

    private DashboardResumenResponse construirResumenDocente(UUID docenteId, String rol, String nombre) {
        List<Curso> cursos = cursoRepository.findByDocenteId(docenteId);
        long totalCursos = cursoRepository.count();

        long totalEstudiantes = 0;
        for (Curso c : cursos) {
            totalEstudiantes += matriculaRepository.countByCursoIdAndEstadoIn(c.getId(), Set.of(EstadoMatricula.ACTIVA, EstadoMatricula.RIESGO));
        }

        MetricasDto metricas = new MetricasDto(
                totalCursos,
                BigDecimal.ZERO,
                totalEstudiantes,
                BigDecimal.ZERO,
                0.0
        );

        Set<UUID> cursoIds = cursos.stream().map(Curso::getId).collect(Collectors.toSet());
        List<ClaseHoyDto> clasesHoy = obtenerClasesHoyOProximas();
        List<TareaPendienteDto> tareasPendientes = obtenerTareasPendientes(cursoIds, true, Set.of());

        return new DashboardResumenResponse(metricas, clasesHoy, tareasPendientes, rol, nombre);
    }

    private List<ClaseHoyDto> obtenerClasesHoyOProximas() {
        LocalDate hoy = LocalDate.now(AppTime.ZONA_NEGOCIO);
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("HH:mm");
        return agenda.obtener(hoy,hoy).stream().filter(e -> e.tipo().equals("CLASE") || e.tipo().equals("CURSO"))
                .map(e -> new ClaseHoyDto(e.id(),e.titulo(),e.cursoTitulo(),e.inicio().format(formato)
                        + (e.fin()==null?"":" - "+e.fin().format(formato)), e.docenteNombre(),e.url(),e.estado().equals("EN_CURSO"))).toList();
    }

    private List<TareaPendienteDto> obtenerTareasPendientes(Set<UUID> cursoIds, boolean gestor, Set<UUID> entregadas) {
        LocalDateTime ahora = LocalDateTime.now(AppTime.ZONA_NEGOCIO);
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd MMM, HH:mm");
        return tareaRepository.findByFechaLimiteBetween(ahora, ahora.plusDays(10)).stream()
                .filter(t -> cursoIds.contains(t.getSemana().getUnidad().getCurso().getId()))
                .filter(t -> gestor || Boolean.TRUE.equals(t.getPublicado()) && Boolean.TRUE.equals(t.getVisible()))
                .filter(t -> !entregadas.contains(t.getId()))
                .map(t -> new TareaPendienteDto(t.getId(), t.getTitulo(), t.getSemana().getUnidad().getCurso().getTitulo(),
                        t.getFechaLimite().format(formato), t.getFechaLimite().isBefore(ahora.plusHours(24)), "PENDIENTE"))
                .toList();
    }
}
