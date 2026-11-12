package com.kellyacademy.enrollment.service;

import com.kellyacademy.attendance.enums.EstadoAsistencia;
import com.kellyacademy.attendance.repository.AsistenciaRepository;
import com.kellyacademy.course.entity.Curso;
import com.kellyacademy.course.repository.ClaseRepository;
import com.kellyacademy.enrollment.entity.Matricula;
import com.kellyacademy.enrollment.enums.EstadoMatricula;
import com.kellyacademy.user.entity.Usuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsistenciaActualServiceTest {
    @Mock ClaseRepository clases;
    @Mock AsistenciaRepository asistencias;
    @InjectMocks AsistenciaActualService servicio;

    @Test
    void pasoDelTiempoActualizaPorcentajeSinModificarLaMatricula() {
        UUID cursoId = UUID.randomUUID(), estudianteId = UUID.randomUUID();
        var curso = new Curso(); curso.setId(cursoId);
        var estudiante = new Usuario(); estudiante.setId(estudianteId);
        var matricula = new Matricula(); matricula.setId(UUID.randomUUID());
        matricula.setCurso(curso); matricula.setEstudiante(estudiante); matricula.setEstado(EstadoMatricula.ACTIVA);
        var antes = LocalDateTime.of(2026, 11, 10, 9, 0);
        var despues = antes.plusHours(2);
        var clase = mock(ClaseRepository.ConteoPorCurso.class);
        when(clase.getCursoId()).thenReturn(cursoId);
        when(clase.getTotal()).thenReturn(1L);
        when(clases.contarDictadasPorCursos(Set.of(cursoId), antes)).thenReturn(List.of());
        when(clases.contarDictadasPorCursos(Set.of(cursoId), despues)).thenReturn(List.of(clase));
        when(asistencias.contarDictadasPorCursosYEstudiantes(eq(Set.of(cursoId)), eq(Set.of(estudianteId)), any()))
                .thenReturn(List.of());

        assertThat(servicio.calcularEn(List.of(matricula), antes)).containsEntry(matricula.getId(), null);
        assertThat(servicio.calcularEn(List.of(matricula), despues).get(matricula.getId())).isEqualByComparingTo("0.00");
        assertThat(matricula.getAsistenciaPorcentaje()).isNull();
        verify(clases, times(2)).contarDictadasPorCursos(any(), any());
        verify(asistencias, times(2)).contarDictadasPorCursosYEstudiantes(any(), any(), any());
    }

    @Test
    void calculaTardanzasYJustificadosYOmiteMatriculasTerminales() {
        UUID cursoId = UUID.randomUUID(), estudianteId = UUID.randomUUID();
        var curso = new Curso(); curso.setId(cursoId);
        var estudiante = new Usuario(); estudiante.setId(estudianteId);
        var activa = new Matricula(); activa.setId(UUID.randomUUID()); activa.setCurso(curso);
        activa.setEstudiante(estudiante); activa.setEstado(EstadoMatricula.RIESGO);
        var terminal = new Matricula(); terminal.setId(UUID.randomUUID()); terminal.setEstado(EstadoMatricula.COMPLETADA);
        var ahora = LocalDateTime.of(2026, 11, 10, 10, 0);
        var total = mock(ClaseRepository.ConteoPorCurso.class);
        when(total.getCursoId()).thenReturn(cursoId); when(total.getTotal()).thenReturn(4L);
        when(clases.contarDictadasPorCursos(Set.of(cursoId), ahora)).thenReturn(List.of(total));
        var presente = conteo(cursoId, estudianteId, EstadoAsistencia.PRESENTE, 1L);
        var tarde = conteo(cursoId, estudianteId, EstadoAsistencia.TARDE, 1L);
        var justificado = conteo(cursoId, estudianteId, EstadoAsistencia.JUSTIFICADO, 1L);
        when(asistencias.contarDictadasPorCursosYEstudiantes(Set.of(cursoId), Set.of(estudianteId), ahora))
                .thenReturn(List.of(presente, tarde, justificado));

        var resultado = servicio.calcularEn(List.of(activa, terminal), ahora);
        assertThat(resultado.get(activa.getId())).isEqualByComparingTo("50.00");
        assertThat(resultado).doesNotContainKey(terminal.getId());
    }

    private AsistenciaRepository.ConteoPorCursoEstudianteEstado conteo(UUID cursoId, UUID estudianteId,
                                                                       EstadoAsistencia estado, long total) {
        var c = mock(AsistenciaRepository.ConteoPorCursoEstudianteEstado.class);
        when(c.getCursoId()).thenReturn(cursoId); when(c.getEstudianteId()).thenReturn(estudianteId);
        when(c.getEstado()).thenReturn(estado); when(c.getTotal()).thenReturn(total);
        return c;
    }
}
