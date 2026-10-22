package com.kellyacademy.dashboard.service;

import com.kellyacademy.calendar.service.AgendaService;
import com.kellyacademy.course.repository.*;
import com.kellyacademy.enrollment.repository.*;
import com.kellyacademy.enrollment.enums.EstadoMatricula;
import com.kellyacademy.support.TestData;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {
    @Mock MatriculaRepository matriculas; @Mock CursoRepository cursos;
    @Mock EntregaRepository entregas; @Mock TareaRepository tareas; @Mock AgendaService agenda;
    @InjectMocks DashboardService service;
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    @Test void alumnoCuentaSoloSusCursosYExcluyeMatriculasAbandonadas() {
        var alumno = TestData.usuario("ESTUDIANTE"); TestData.autenticar(alumno);
        var propio = TestData.curso(TestData.usuario("DOCENTE"));
        var activo = TestData.matricula(propio, alumno, EstadoMatricula.ACTIVA); activo.setNotaFinal(new BigDecimal("85")); activo.setAsistenciaPorcentaje(new BigDecimal("90"));
        var retirado = TestData.matricula(TestData.curso(propio.getDocente()), alumno, EstadoMatricula.ABANDONADA);
        when(matriculas.findByEstudianteId(alumno.getId())).thenReturn(List.of(activo, retirado));
        var result = service.obtenerResumen();
        assertThat(result.metricas().cursosInscritos()).isEqualTo(1);
        assertThat(result.metricas().promedioCalificacion()).isEqualByComparingTo("85");
        assertThat(result.metricas().porcentajeAsistencia()).isEqualByComparingTo("90");
        assertThat(result.rolUsuario()).isEqualTo("ESTUDIANTE"); verifyNoInteractions(cursos);
    }
    @Test void docenteConservaConteoGlobalDeCursos() {
        var docente = TestData.usuario("DOCENTE"); TestData.autenticar(docente);
        when(cursos.count()).thenReturn(10L);
        var result = service.obtenerResumen();
        assertThat(result.metricas().cursosInscritos()).isEqualTo(10); assertThat(result.rolUsuario()).isEqualTo("DOCENTE");
    }
    @Test void alumnoSinCursosRecibeCerosYNoEstadisticasGlobales() {
        TestData.autenticar(TestData.usuario("ESTUDIANTE"));
        var result = service.obtenerResumen();
        assertThat(result.metricas().cursosInscritos()).isZero(); assertThat(result.metricas().promedioCalificacion()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.tareasPendientes()).isEmpty(); verifyNoInteractions(cursos);
    }
    @Test void tareasPendientesNoIncluyenBorradoresNiCursosAjenos() {
        var alumno = TestData.usuario("ESTUDIANTE"); TestData.autenticar(alumno);
        var propio = TestData.curso(TestData.usuario("DOCENTE"));
        when(matriculas.findByEstudianteId(alumno.getId())).thenReturn(List.of(TestData.matricula(propio, alumno, EstadoMatricula.ACTIVA)));
        var visible = TestData.tarea(propio); visible.setFechaLimite(LocalDateTime.now().plusDays(3));
        var oculto = TestData.tarea(propio); oculto.setPublicado(false);
        var ajeno = TestData.tarea(TestData.curso(propio.getDocente()));
        when(tareas.findByFechaLimiteBetween(any(), any())).thenReturn(List.of(visible, oculto, ajeno));
        assertThat(service.obtenerResumen().tareasPendientes()).singleElement().satisfies(t -> assertThat(t.id()).isEqualTo(visible.getId()));
    }
}
