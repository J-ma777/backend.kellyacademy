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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final MatriculaRepository matriculaRepository;
    private final CursoRepository cursoRepository;
    private final EntregaRepository entregaRepository;
    private final TareaRepository tareaRepository;
    private final ClaseRepository claseRepository;

    public DashboardResumenResponse obtenerResumen() {
        CustomUserDetails userDetails = SecurityUtils.getUsuarioAutenticado();
        Usuario usuario = userDetails.getUsuario();
        UUID usuarioId = usuario.getId();
        boolean esDocente = userDetails.getAuthorities().stream()
                .anyMatch(a -> "ROLE_DOCENTE".equals(a.getAuthority()));
        boolean esAdmin = SecurityUtils.esAdmin();

        String rol = esAdmin ? "ADMINISTRADOR" : (esDocente ? "DOCENTE" : "ESTUDIANTE");
        String nombre = usuario.getNombre() + (usuario.getApellido() != null ? " " + usuario.getApellido() : "");

        if (esDocente) {
            return construirResumenDocente(usuarioId, rol, nombre.trim());
        } else {
            return construirResumenEstudiante(usuarioId, rol, nombre.trim());
        }
    }

    private DashboardResumenResponse construirResumenEstudiante(UUID estudianteId, String rol, String nombre) {
        List<Matricula> matriculas = matriculaRepository.findByEstudianteId(estudianteId);
        List<Matricula> matriculasActivas = matriculas.stream()
                .filter(m -> m.getEstado() == EstadoMatricula.ACTIVA)
                .toList();

        long totalCursos = matriculasActivas.size();

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
        List<ClaseHoyDto> clasesHoy = obtenerClasesHoyOProximas();

        // Tareas pendientes
        List<TareaPendienteDto> tareasPendientes = obtenerTareasPendientes();

        return new DashboardResumenResponse(metricas, clasesHoy, tareasPendientes, rol, nombre);
    }

    private DashboardResumenResponse construirResumenDocente(UUID docenteId, String rol, String nombre) {
        List<Curso> cursos = cursoRepository.findByDocenteId(docenteId);
        long totalCursos = cursos.size();

        long totalEstudiantes = 0;
        for (Curso c : cursos) {
            totalEstudiantes += matriculaRepository.countByCursoIdAndEstado(c.getId(), EstadoMatricula.ACTIVA);
        }

        MetricasDto metricas = new MetricasDto(
                totalCursos,
                BigDecimal.ZERO,
                totalEstudiantes,
                BigDecimal.ZERO,
                0.0
        );

        List<ClaseHoyDto> clasesHoy = obtenerClasesHoyOProximas();
        List<TareaPendienteDto> tareasPendientes = obtenerTareasPendientes();

        return new DashboardResumenResponse(metricas, clasesHoy, tareasPendientes, rol, nombre);
    }

    private List<ClaseHoyDto> obtenerClasesHoyOProximas() {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime inicioDia = LocalDate.now().atStartOfDay();
        LocalDateTime finDia = LocalDate.now().atTime(23, 59, 59);

        List<Clase> clases = claseRepository.findByFechaHoraBetween(inicioDia, finDia);
        List<ClaseHoyDto> dtos = new ArrayList<>();

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        for (Clase c : clases) {
            String horaInicio = c.getFechaHora() != null ? c.getFechaHora().format(timeFormatter) : "18:00";
            String horaFin = c.getFechaHora() != null ? c.getFechaHora().plusMinutes(90).format(timeFormatter) : "19:30";
            String cursoTitulo = "English Course";
            try {
                if (c.getSemana() != null && c.getSemana().getUnidad() != null && c.getSemana().getUnidad().getCurso() != null) {
                    cursoTitulo = c.getSemana().getUnidad().getCurso().getTitulo();
                }
            } catch (Exception ignored) {}

            dtos.add(new ClaseHoyDto(
                    c.getId(),
                    c.getTitulo(),
                    cursoTitulo,
                    horaInicio + " - " + horaFin,
                    "Prof. Kelly M.",
                    c.getUrlVivo() != null ? c.getUrlVivo() : "https://meet.google.com/kel-acad-live",
                    true
            ));
        }

        return dtos;
    }

    private List<TareaPendienteDto> obtenerTareasPendientes() {
        LocalDateTime ahora = LocalDateTime.now();
        List<Tarea> tareas = tareaRepository.findByFechaLimiteBetween(ahora, ahora.plusDays(10));
        List<TareaPendienteDto> dtos = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM, HH:mm");

        for (Tarea t : tareas) {
            String cursoTitulo = "Curso General";
            try {
                if (t.getSemana() != null && t.getSemana().getUnidad() != null && t.getSemana().getUnidad().getCurso() != null) {
                    cursoTitulo = t.getSemana().getUnidad().getCurso().getTitulo();
                }
            } catch (Exception ignored) {}

            dtos.add(new TareaPendienteDto(
                    t.getId(),
                    t.getTitulo(),
                    cursoTitulo,
                    t.getFechaLimite() != null ? t.getFechaLimite().format(formatter) : "Próximamente",
                    t.getFechaLimite() != null && t.getFechaLimite().isBefore(ahora.plusHours(24)),
                    "PENDIENTE"
            ));
        }

        return dtos;
    }
}
