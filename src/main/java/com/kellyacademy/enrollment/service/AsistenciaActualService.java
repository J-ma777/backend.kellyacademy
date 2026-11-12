package com.kellyacademy.enrollment.service;

import com.kellyacademy.attendance.enums.EstadoAsistencia;
import com.kellyacademy.attendance.repository.AsistenciaRepository;
import com.kellyacademy.course.repository.ClaseRepository;
import com.kellyacademy.enrollment.dto.response.MatriculaResponse;
import com.kellyacademy.enrollment.entity.Matricula;
import com.kellyacademy.enrollment.enums.EstadoMatricula;
import com.kellyacademy.shared.config.AppTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Calcula la asistencia vigente en la lectura, sin modificar las matrículas. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AsistenciaActualService {
    private final ClaseRepository clases;
    private final AsistenciaRepository asistencias;

    public Map<UUID, BigDecimal> calcular(Collection<Matricula> matriculas) {
        return calcularEn(matriculas, LocalDateTime.now(AppTime.ZONA_NEGOCIO));
    }

    Map<UUID, BigDecimal> calcularEn(Collection<Matricula> matriculas, LocalDateTime ahora) {
        List<Matricula> vigentes = matriculas.stream()
                .filter(m -> m.getEstado() == EstadoMatricula.ACTIVA || m.getEstado() == EstadoMatricula.RIESGO)
                .toList();
        if (vigentes.isEmpty()) return Map.of();
        Set<UUID> cursoIds = vigentes.stream().map(m -> m.getCurso().getId()).collect(Collectors.toSet());
        Set<UUID> estudianteIds = vigentes.stream().map(m -> m.getEstudiante().getId()).collect(Collectors.toSet());

        Map<UUID, Long> dictadas = new HashMap<>();
        clases.contarDictadasPorCursos(cursoIds, ahora).forEach(c -> dictadas.put(c.getCursoId(), c.getTotal()));
        Map<Clave, EnumMap<EstadoAsistencia, Long>> registros = new HashMap<>();
        asistencias.contarDictadasPorCursosYEstudiantes(cursoIds, estudianteIds, ahora).forEach(a ->
                registros.computeIfAbsent(new Clave(a.getCursoId(), a.getEstudianteId()),
                        k -> new EnumMap<>(EstadoAsistencia.class)).put(a.getEstado(), a.getTotal()));

        Map<UUID, BigDecimal> resultado = new HashMap<>();
        for (Matricula m : vigentes) {
            UUID cursoId = m.getCurso().getId();
            var conteos = registros.getOrDefault(new Clave(cursoId, m.getEstudiante().getId()),
                    new EnumMap<>(EstadoAsistencia.class));
            BigDecimal valor = CalculoAsistencia.porcentaje(
                    dictadas.getOrDefault(cursoId, 0L),
                    conteos.getOrDefault(EstadoAsistencia.PRESENTE, 0L),
                    conteos.getOrDefault(EstadoAsistencia.TARDE, 0L),
                    conteos.getOrDefault(EstadoAsistencia.JUSTIFICADO, 0L));
            resultado.put(m.getId(), valor);
        }
        return resultado;
    }

    public static MatriculaResponse aplicar(MatriculaResponse r, Map<UUID, BigDecimal> calculados) {
        if (r == null || !calculados.containsKey(r.id())) return r;
        return new MatriculaResponse(r.id(), r.cursoId(), r.cursoTitulo(), r.estudianteId(),
                r.estudianteNombreCompleto(), r.estudianteCorreo(), r.estado(), r.notaFinal(),
                calculados.get(r.id()), r.matriculadoAt(), r.fechaCreacion(), r.fechaActualizacion());
    }

    private record Clave(UUID cursoId, UUID estudianteId) {}
}
