package com.kellyacademy.enrollment.service;

import com.kellyacademy.attendance.enums.EstadoAsistencia;
import com.kellyacademy.attendance.repository.AsistenciaRepository;
import com.kellyacademy.course.entity.Curso;
import com.kellyacademy.course.repository.ClaseRepository;
import com.kellyacademy.enrollment.entity.Matricula;
import com.kellyacademy.enrollment.enums.EstadoMatricula;
import com.kellyacademy.enrollment.repository.EntregaRepository;
import com.kellyacademy.enrollment.repository.MatriculaRepository;
import com.kellyacademy.enrollment.repository.NotaPuntajeProjection;
import com.kellyacademy.shared.exception.ResourceNotFoundException;
import com.kellyacademy.user.entity.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalculoMatriculaServiceTest {

    @Mock private MatriculaRepository matriculaRepository;
    @Mock private EntregaRepository entregaRepository;
    @Mock private ClaseRepository claseRepository;
    @Mock private AsistenciaRepository asistenciaRepository;

    @InjectMocks private CalculoMatriculaService calculoMatriculaService;

    private UUID matriculaId;
    private UUID cursoId;
    private UUID estudianteId;
    private Matricula matricula;

    @BeforeEach
    void setUp() {
        matriculaId = UUID.randomUUID();
        cursoId = UUID.randomUUID();
        estudianteId = UUID.randomUUID();

        Curso curso = new Curso();
        curso.setId(cursoId);

        Usuario estudiante = new Usuario();
        estudiante.setId(estudianteId);

        matricula = new Matricula();
        matricula.setId(matriculaId);
        matricula.setCurso(curso);
        matricula.setEstudiante(estudiante);
        matricula.setEstado(EstadoMatricula.ACTIVA);
    }

    private NotaPuntajeProjection crearProyeccion(BigDecimal nota, Integer puntajeMaximo) {
        return new NotaPuntajeProjection() {
            @Override
            public BigDecimal getNota() {
                return nota;
            }

            @Override
            public Integer getPuntajeMaximo() {
                return puntajeMaximo;
            }
        };
    }

    @Test
    void recalcular_notaFinalConUnaSolaEntregaSobrePuntajeMaximo100() {
        when(matriculaRepository.findWithCursoAndEstudianteById(matriculaId)).thenReturn(Optional.of(matricula));
        when(entregaRepository.findNotasCalificadasPorCursoYEstudiante(cursoId, estudianteId))
                .thenReturn(List.of(crearProyeccion(new BigDecimal("85.00"), 100)));
        when(claseRepository.contarClasesDictadasPorCurso(eq(cursoId), any(LocalDateTime.class))).thenReturn(0L);

        calculoMatriculaService.recalcular(matriculaId);

        assertThat(matricula.getNotaFinal()).isEqualByComparingTo(new BigDecimal("85.00"));
        verify(matriculaRepository).save(matricula);
    }

    @Test
    void recalcular_notaFinalConUnaSolaEntregaSobrePuntajeMaximo20_normalizaA100() {
        when(matriculaRepository.findWithCursoAndEstudianteById(matriculaId)).thenReturn(Optional.of(matricula));
        when(entregaRepository.findNotasCalificadasPorCursoYEstudiante(cursoId, estudianteId))
                .thenReturn(List.of(crearProyeccion(new BigDecimal("18.00"), 20)));
        when(claseRepository.contarClasesDictadasPorCurso(eq(cursoId), any(LocalDateTime.class))).thenReturn(0L);

        calculoMatriculaService.recalcular(matriculaId);

        assertThat(matricula.getNotaFinal()).isEqualByComparingTo(new BigDecimal("90.00"));
        verify(matriculaRepository).save(matricula);
    }

    @Test
    void recalcular_notaFinalConVariasEntregasDeDistintoPuntajeMaximo_formulaMixta() {
        when(matriculaRepository.findWithCursoAndEstudianteById(matriculaId)).thenReturn(Optional.of(matricula));
        // Entrega 1: 18/20, Entrega 2: 80/100 -> sumaNotas = 98, sumaMax = 120 -> 9800/120 = 81.666... -> 81.67
        when(entregaRepository.findNotasCalificadasPorCursoYEstudiante(cursoId, estudianteId))
                .thenReturn(List.of(
                        crearProyeccion(new BigDecimal("18.00"), 20),
                        crearProyeccion(new BigDecimal("80.00"), 100)
                ));
        when(claseRepository.contarClasesDictadasPorCurso(eq(cursoId), any(LocalDateTime.class))).thenReturn(0L);

        calculoMatriculaService.recalcular(matriculaId);

        assertThat(matricula.getNotaFinal()).isEqualByComparingTo(new BigDecimal("81.67"));
        verify(matriculaRepository).save(matricula);
    }

    @Test
    void recalcular_notaFinalNull_cuandoNoHayCalificadas() {
        when(matriculaRepository.findWithCursoAndEstudianteById(matriculaId)).thenReturn(Optional.of(matricula));
        when(entregaRepository.findNotasCalificadasPorCursoYEstudiante(cursoId, estudianteId))
                .thenReturn(List.of());
        when(claseRepository.contarClasesDictadasPorCurso(eq(cursoId), any(LocalDateTime.class))).thenReturn(0L);

        calculoMatriculaService.recalcular(matriculaId);

        assertThat(matricula.getNotaFinal()).isNull();
        verify(matriculaRepository).save(matricula);
    }

    @Test
    void recalcular_asistenciaPorcentajeConPresenteTardeAusente() {
        when(matriculaRepository.findWithCursoAndEstudianteById(matriculaId)).thenReturn(Optional.of(matricula));
        when(entregaRepository.findNotasCalificadasPorCursoYEstudiante(cursoId, estudianteId)).thenReturn(List.of());
        // 10 clases dictadas, 7 presentes, 2 tardes (ausente implicito: 1) -> num = 7 + 1.0 = 8.0, den = 10 -> 80.00%
        when(claseRepository.contarClasesDictadasPorCurso(eq(cursoId), any(LocalDateTime.class))).thenReturn(10L);
        when(asistenciaRepository.contarPorCursoEstudianteYEstado(cursoId, estudianteId, EstadoAsistencia.PRESENTE))
                .thenReturn(7L);
        when(asistenciaRepository.contarPorCursoEstudianteYEstado(cursoId, estudianteId, EstadoAsistencia.TARDE))
                .thenReturn(2L);
        when(asistenciaRepository.contarPorCursoEstudianteYEstado(cursoId, estudianteId, EstadoAsistencia.JUSTIFICADO))
                .thenReturn(0L);

        calculoMatriculaService.recalcular(matriculaId);

        assertThat(matricula.getAsistenciaPorcentaje()).isEqualByComparingTo(new BigDecimal("80.00"));
        verify(matriculaRepository).save(matricula);
    }

    @Test
    void recalcular_asistenciaPorcentajeNeutralizaJustificadoEnDenominador() {
        when(matriculaRepository.findWithCursoAndEstudianteById(matriculaId)).thenReturn(Optional.of(matricula));
        when(entregaRepository.findNotasCalificadasPorCursoYEstudiante(cursoId, estudianteId)).thenReturn(List.of());
        // 10 clases dictadas, 6 presentes, 2 tardes, 2 justificados -> num = 6 + 1.0 = 7.0, den = 10 - 2 = 8 -> 87.50%
        when(claseRepository.contarClasesDictadasPorCurso(eq(cursoId), any(LocalDateTime.class))).thenReturn(10L);
        when(asistenciaRepository.contarPorCursoEstudianteYEstado(cursoId, estudianteId, EstadoAsistencia.PRESENTE))
                .thenReturn(6L);
        when(asistenciaRepository.contarPorCursoEstudianteYEstado(cursoId, estudianteId, EstadoAsistencia.TARDE))
                .thenReturn(2L);
        when(asistenciaRepository.contarPorCursoEstudianteYEstado(cursoId, estudianteId, EstadoAsistencia.JUSTIFICADO))
                .thenReturn(2L);

        calculoMatriculaService.recalcular(matriculaId);

        assertThat(matricula.getAsistenciaPorcentaje()).isEqualByComparingTo(new BigDecimal("87.50"));
        verify(matriculaRepository).save(matricula);
    }

    @Test
    void recalcular_asistenciaPorcentajeNull_cuandoNoHayClasesDictadas() {
        when(matriculaRepository.findWithCursoAndEstudianteById(matriculaId)).thenReturn(Optional.of(matricula));
        when(entregaRepository.findNotasCalificadasPorCursoYEstudiante(cursoId, estudianteId)).thenReturn(List.of());
        when(claseRepository.contarClasesDictadasPorCurso(eq(cursoId), any(LocalDateTime.class))).thenReturn(0L);

        calculoMatriculaService.recalcular(matriculaId);

        assertThat(matricula.getAsistenciaPorcentaje()).isNull();
        verify(matriculaRepository).save(matricula);
    }

    @Test
    void recalcular_asistenciaPorcentajeNull_cuandoTodasEstanJustificado() {
        when(matriculaRepository.findWithCursoAndEstudianteById(matriculaId)).thenReturn(Optional.of(matricula));
        when(entregaRepository.findNotasCalificadasPorCursoYEstudiante(cursoId, estudianteId)).thenReturn(List.of());
        // 3 clases dictadas, 3 justificados -> den = 3 - 3 = 0 -> null
        when(claseRepository.contarClasesDictadasPorCurso(eq(cursoId), any(LocalDateTime.class))).thenReturn(3L);
        when(asistenciaRepository.contarPorCursoEstudianteYEstado(cursoId, estudianteId, EstadoAsistencia.PRESENTE))
                .thenReturn(0L);
        when(asistenciaRepository.contarPorCursoEstudianteYEstado(cursoId, estudianteId, EstadoAsistencia.TARDE))
                .thenReturn(0L);
        when(asistenciaRepository.contarPorCursoEstudianteYEstado(cursoId, estudianteId, EstadoAsistencia.JUSTIFICADO))
                .thenReturn(3L);

        calculoMatriculaService.recalcular(matriculaId);

        assertThat(matricula.getAsistenciaPorcentaje()).isNull();
        verify(matriculaRepository).save(matricula);
    }

    @Test
    void recalcular_matriculaCompletada_noRecalculaEnRecalcularAutomatico() {
        matricula.setEstado(EstadoMatricula.COMPLETADA);
        when(matriculaRepository.findWithCursoAndEstudianteById(matriculaId)).thenReturn(Optional.of(matricula));

        calculoMatriculaService.recalcular(matriculaId);

        verify(matriculaRepository, never()).save(any());
        verifyNoInteractions(entregaRepository);
        verifyNoInteractions(claseRepository);
        verifyNoInteractions(asistenciaRepository);
    }

    @Test
    void recalcular_matriculaAbandonada_noRecalculaEnRecalcularAutomatico() {
        matricula.setEstado(EstadoMatricula.ABANDONADA);
        when(matriculaRepository.findWithCursoAndEstudianteById(matriculaId)).thenReturn(Optional.of(matricula));

        calculoMatriculaService.recalcular(matriculaId);

        verify(matriculaRepository, never()).save(any());
        verifyNoInteractions(entregaRepository);
        verifyNoInteractions(claseRepository);
        verifyNoInteractions(asistenciaRepository);
    }

    @Test
    void recalcularIgnorandoEstado_siRecalculaEnCompletada() {
        matricula.setEstado(EstadoMatricula.COMPLETADA);
        when(matriculaRepository.findWithCursoAndEstudianteById(matriculaId)).thenReturn(Optional.of(matricula));
        when(entregaRepository.findNotasCalificadasPorCursoYEstudiante(cursoId, estudianteId))
                .thenReturn(List.of(crearProyeccion(new BigDecimal("95.00"), 100)));
        when(claseRepository.contarClasesDictadasPorCurso(eq(cursoId), any(LocalDateTime.class))).thenReturn(0L);

        calculoMatriculaService.recalcularIgnorandoEstado(matriculaId);

        assertThat(matricula.getNotaFinal()).isEqualByComparingTo(new BigDecimal("95.00"));
        verify(matriculaRepository).save(matricula);
    }

    @Test
    void recalcular_redondeoHalfUpDosDecimales() {
        when(matriculaRepository.findWithCursoAndEstudianteById(matriculaId)).thenReturn(Optional.of(matricula));
        // 49 / 60 * 100 = 81.6666... -> 81.67
        when(entregaRepository.findNotasCalificadasPorCursoYEstudiante(cursoId, estudianteId))
                .thenReturn(List.of(crearProyeccion(new BigDecimal("49.00"), 60)));
        when(claseRepository.contarClasesDictadasPorCurso(eq(cursoId), any(LocalDateTime.class))).thenReturn(0L);

        calculoMatriculaService.recalcular(matriculaId);

        assertThat(matricula.getNotaFinal()).isEqualByComparingTo(new BigDecimal("81.67"));
        verify(matriculaRepository).save(matricula);
    }

    @Test
    void recalcular_matriculaInexistente_lanzaResourceNotFound() {
        when(matriculaRepository.findWithCursoAndEstudianteById(matriculaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> calculoMatriculaService.recalcular(matriculaId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Matricula");
    }
}

