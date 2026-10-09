package com.kellyacademy.enrollment.service;

import com.kellyacademy.attendance.enums.EstadoAsistencia;
import com.kellyacademy.attendance.repository.AsistenciaRepository;
import com.kellyacademy.course.repository.ClaseRepository;
import com.kellyacademy.enrollment.entity.Matricula;
import com.kellyacademy.enrollment.enums.EstadoMatricula;
import com.kellyacademy.enrollment.repository.EntregaRepository;
import com.kellyacademy.enrollment.repository.MatriculaRepository;
import com.kellyacademy.enrollment.repository.NotaPuntajeProjection;
import com.kellyacademy.shared.config.AppTime;
import com.kellyacademy.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CalculoMatriculaService {

    private static final String RECURSO_MATRICULA = "Matricula";
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final BigDecimal PESO_TARDE = new BigDecimal("0.5");

    private final MatriculaRepository matriculaRepository;
    private final EntregaRepository entregaRepository;
    private final ClaseRepository claseRepository;
    private final AsistenciaRepository asistenciaRepository;

    public void recalcular(UUID matriculaId) {
        Matricula matricula = matriculaRepository.findWithCursoAndEstudianteById(matriculaId)
                .orElseThrow(() -> new ResourceNotFoundException(RECURSO_MATRICULA, "id", matriculaId));

        if (matricula.getEstado() == EstadoMatricula.COMPLETADA
                || matricula.getEstado() == EstadoMatricula.ABANDONADA) {
            return;
        }

        ejecutarCalculo(matricula);
    }

    public void recalcularIgnorandoEstado(UUID matriculaId) {
        Matricula matricula = matriculaRepository.findWithCursoAndEstudianteById(matriculaId)
                .orElseThrow(() -> new ResourceNotFoundException(RECURSO_MATRICULA, "id", matriculaId));

        ejecutarCalculo(matricula);
    }

    private void ejecutarCalculo(Matricula matricula) {
        UUID cursoId = matricula.getCurso().getId();
        UUID estudianteId = matricula.getEstudiante().getId();

        BigDecimal notaFinal = calcularNotaFinal(cursoId, estudianteId);
        BigDecimal asistenciaPorcentaje = calcularAsistenciaPorcentaje(cursoId, estudianteId);

        matricula.setNotaFinal(notaFinal);
        matricula.setAsistenciaPorcentaje(asistenciaPorcentaje);
        matriculaRepository.save(matricula);
    }

    private BigDecimal calcularNotaFinal(UUID cursoId, UUID estudianteId) {
        List<NotaPuntajeProjection> entregas = entregaRepository
                .findNotasCalificadasPorCursoYEstudiante(cursoId, estudianteId);

        if (entregas.isEmpty()) {
            return null;
        }

        BigDecimal sumaNotas = BigDecimal.ZERO;
        BigDecimal sumaMaximos = BigDecimal.ZERO;

        for (NotaPuntajeProjection entrega : entregas) {
            if (entrega.getNota() != null && entrega.getPuntajeMaximo() != null && entrega.getPuntajeMaximo() > 0) {
                sumaNotas = sumaNotas.add(entrega.getNota());
                sumaMaximos = sumaMaximos.add(BigDecimal.valueOf(entrega.getPuntajeMaximo()));
            }
        }

        if (sumaMaximos.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        return sumaNotas
                .multiply(CIEN)
                .divide(sumaMaximos, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calcularAsistenciaPorcentaje(UUID cursoId, UUID estudianteId) {
        LocalDateTime ahora = LocalDateTime.now(AppTime.ZONA_NEGOCIO);
        long totalClasesDictadas = claseRepository.contarClasesDictadasPorCurso(cursoId, ahora);

        long presentes = asistenciaRepository.contarPorCursoEstudianteYEstado(
                cursoId, estudianteId, EstadoAsistencia.PRESENTE
        );
        long tardes = asistenciaRepository.contarPorCursoEstudianteYEstado(
                cursoId, estudianteId, EstadoAsistencia.TARDE
        );
        long justificados = asistenciaRepository.contarPorCursoEstudianteYEstado(
                cursoId, estudianteId, EstadoAsistencia.JUSTIFICADO
        );

        long denominador = totalClasesDictadas - justificados;
        if (denominador <= 0) {
            return null;
        }

        BigDecimal numPresentes = BigDecimal.valueOf(presentes);
        BigDecimal numTardes = BigDecimal.valueOf(tardes).multiply(PESO_TARDE);
        BigDecimal numerador = numPresentes.add(numTardes);

        return numerador
                .multiply(CIEN)
                .divide(BigDecimal.valueOf(denominador), 2, RoundingMode.HALF_UP);
    }
}

