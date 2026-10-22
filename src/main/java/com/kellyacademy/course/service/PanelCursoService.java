package com.kellyacademy.course.service;

import com.kellyacademy.communication.entity.Anuncio;
import com.kellyacademy.communication.mapper.AnuncioMapper;
import com.kellyacademy.communication.repository.AnuncioRepository;
import com.kellyacademy.course.dto.response.PanelCursoResponse;
import com.kellyacademy.course.dto.response.PanelCursoResponse.*;
import com.kellyacademy.course.entity.*;
import com.kellyacademy.course.mapper.*;
import com.kellyacademy.course.repository.*;
import com.kellyacademy.enrollment.dto.response.MatriculaResponse;
import com.kellyacademy.enrollment.entity.Matricula;
import com.kellyacademy.enrollment.enums.EstadoMatricula;
import com.kellyacademy.enrollment.mapper.EntregaMapper;
import com.kellyacademy.enrollment.mapper.MatriculaMapper;
import com.kellyacademy.enrollment.repository.EntregaRepository;
import com.kellyacademy.enrollment.repository.MatriculaRepository;
import com.kellyacademy.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PanelCursoService {
    private final AccesoCursoService acceso;
    private final UnidadRepository unidadRepository;
    private final SemanaRepository semanaRepository;
    private final ClaseRepository claseRepository;
    private final MaterialRepository materialRepository;
    private final TareaRepository tareaRepository;
    private final MatriculaRepository matriculaRepository;
    private final AnuncioRepository anuncioRepository;
    private final EntregaRepository entregaRepository;
    private final ProgresoContenidoRepository progresoRepository;
    private final CursoMapper cursoMapper;
    private final UnidadMapper unidadMapper;
    private final SemanaMapper semanaMapper;
    private final ClaseMapper claseMapper;
    private final MaterialMapper materialMapper;
    private final TareaMapper tareaMapper;
    private final MatriculaMapper matriculaMapper;
    private final AnuncioMapper anuncioMapper;
    private final EntregaMapper entregaMapper;

    public PanelCursoResponse obtener(UUID cursoId) {
        Curso curso = acceso.obtener(cursoId);
        acceso.validarGestor(curso);
        return construir(curso, true);
    }
    public PanelCursoResponse obtenerAula(UUID cursoId) {
        Curso curso = acceso.obtener(cursoId);
        return construir(curso, acceso.esGestor(curso));
    }
    private boolean visible(ContenidoEntity c) { return Boolean.TRUE.equals(c.getPublicado()) && Boolean.TRUE.equals(c.getVisible()); }

    private PanelCursoResponse construir(Curso curso, boolean gestor) {
        UUID cursoId = curso.getId();
        UUID usuarioId = SecurityUtils.getUsuarioAutenticadoId();
        var todasClases = claseRepository.findBySemanaUnidadCursoIdOrderByFechaHoraAsc(cursoId);
        var todosMateriales = materialRepository.findBySemanaUnidadCursoIdOrderByFechaCreacionAsc(cursoId);
        var todasTareas = tareaRepository.findBySemanaUnidadCursoIdOrderByFechaLimiteAsc(cursoId);
        Set<UUID> publicados = new HashSet<>();
        todasClases.stream().filter(this::visible).map(Clase::getId).forEach(publicados::add);
        todosMateriales.stream().filter(this::visible).map(Material::getId).forEach(publicados::add);
        todasTareas.stream().filter(this::visible).map(Tarea::getId).forEach(publicados::add);
        var todasEntregas = entregaRepository.findByTareaSemanaUnidadCursoIdOrderByEnviadoAtDesc(cursoId);
        var matriculas = matriculaRepository.findByCursoIdOrderByEstudianteApellidoAscEstudianteNombreAsc(cursoId);

        Map<UUID, Set<UUID>> revisados = new HashMap<>();
        progresoRepository.findPorCurso(cursoId).forEach(p -> revisados.computeIfAbsent(p.getEstudiante().getId(), k -> new HashSet<>())
                .add(p.getMaterial() != null ? p.getMaterial().getId() : p.getClase().getId()));
        todasEntregas.forEach(e -> revisados.computeIfAbsent(e.getEstudiante().getId(), k -> new HashSet<>()).add(e.getTarea().getId()));
        Map<UUID, BigDecimal> progresos = new HashMap<>();
        for (Matricula m : matriculas) {
            long completados = revisados.getOrDefault(m.getEstudiante().getId(), Set.of()).stream().filter(publicados::contains).count();
            progresos.put(m.getEstudiante().getId(), publicados.isEmpty() ? null : BigDecimal.valueOf(completados * 100)
                    .divide(BigDecimal.valueOf(publicados.size()), 1, RoundingMode.HALF_UP));
        }
        var valores = matriculas.stream().filter(m -> m.getEstado() != EstadoMatricula.ABANDONADA)
                .map(m -> progresos.get(m.getEstudiante().getId())).filter(Objects::nonNull).toList();
        BigDecimal promedio = valores.isEmpty() ? null : valores.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(valores.size()), 1, RoundingMode.HALF_UP);

        Map<UUID, List<Semana>> semanas = semanaRepository.findByUnidadCursoIdOrderByUnidadNumeroAscNumeroAsc(cursoId).stream()
                .collect(Collectors.groupingBy(s -> s.getUnidad().getId()));
        var clases = todasClases.stream().filter(c -> gestor || visible(c)).collect(Collectors.groupingBy(c -> c.getSemana().getId()));
        var materiales = todosMateriales.stream().filter(c -> gestor || visible(c)).collect(Collectors.groupingBy(c -> c.getSemana().getId()));
        var tareas = todasTareas.stream().filter(c -> gestor || visible(c)).collect(Collectors.groupingBy(c -> c.getSemana().getId()));
        var unidades = unidadRepository.findByCursoIdOrderByNumeroAsc(cursoId).stream().map(u -> new UnidadPanel(unidadMapper.toResponse(u),
                semanas.getOrDefault(u.getId(), List.of()).stream().map(s -> new SemanaPanel(semanaMapper.toResponse(s),
                        clases.getOrDefault(s.getId(), List.of()).stream().map(claseMapper::toResponse).toList(),
                        materiales.getOrDefault(s.getId(), List.of()).stream().map(materialMapper::toResponse).toList(),
                        tareas.getOrDefault(s.getId(), List.of()).stream().map(tareaMapper::toResponse).toList()))
                        .filter(s -> gestor || !s.clases().isEmpty() || !s.materiales().isEmpty() || !s.tareas().isEmpty()).toList()))
                .filter(u -> gestor || !u.semanas().isEmpty()).toList();

        var alumnos = matriculas.stream().filter(m -> gestor || m.getEstado() != EstadoMatricula.ABANDONADA)
                .map(m -> {
                    var r = matriculaMapper.toResponse(m);
                    if (gestor || m.getEstudiante().getId().equals(usuarioId)) return r;
                    return new MatriculaResponse(r.id(), r.cursoId(), r.cursoTitulo(), r.estudianteId(), r.estudianteNombreCompleto(),
                            r.estudianteCorreo(), r.estado(), null, null, r.matriculadoAt(), r.fechaCreacion(), r.fechaActualizacion());
                }).toList();
        var avances = matriculas.stream().filter(m -> gestor || m.getEstado() != EstadoMatricula.ABANDONADA)
                .map(m -> new AvanceAlumno(m.getEstudiante().getId(), m.getEstudiante().getAvatarUrl(),
                        gestor || m.getEstudiante().getId().equals(usuarioId) ? progresos.get(m.getEstudiante().getId()) : null)).toList();
        var anuncios = anuncioRepository.findByCursoIdOrderByFechaCreacionDesc(cursoId).stream()
                .filter(a -> gestor || Boolean.TRUE.equals(a.getActivo()) && Boolean.TRUE.equals(a.getPublicado()))
                .sorted(Comparator.comparing(Anuncio::getFijado, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Anuncio::getPublicadoAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Anuncio::getFechaCreacion, Comparator.reverseOrder()))
                .map(anuncioMapper::toResponse).toList();
        long activos = matriculas.stream().filter(m -> m.getEstado() == EstadoMatricula.ACTIVA || m.getEstado() == EstadoMatricula.RIESGO).count();
        return new PanelCursoResponse(cursoMapper.toResponse(curso), unidades, alumnos, anuncios,
                todasEntregas.stream().filter(e -> gestor || e.getEstudiante().getId().equals(usuarioId) && publicados.contains(e.getTarea().getId()))
                        .map(entregaMapper::toResponse).toList(), gestor, avances,
                new Estadisticas(matriculas.size(), activos, promedio, publicados.size()),
                revisados.getOrDefault(usuarioId, Set.of()).stream().filter(publicados::contains).toList());
    }
}
