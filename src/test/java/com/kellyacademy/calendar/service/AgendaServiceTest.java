package com.kellyacademy.calendar.service;

import com.kellyacademy.calendar.repository.*;
import com.kellyacademy.course.entity.*;
import com.kellyacademy.course.enums.EstadoCurso;
import com.kellyacademy.course.repository.*;
import com.kellyacademy.enrollment.enums.EstadoMatricula;
import com.kellyacademy.enrollment.repository.MatriculaRepository;
import com.kellyacademy.shared.exception.BusinessException;
import com.kellyacademy.support.TestData;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class AgendaServiceTest {
    @Mock CursoRepository cursos; @Mock MatriculaRepository matriculas;
    @Mock HorarioCursoRepository horarios; @Mock ClaseRepository clases;
    @Mock TutoriaRepository tutorias; @Mock EventoRepository eventos;
    @InjectMocks AgendaService service;
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    @Test void rechazaRangosInvertidosOExcesivosAntesDeConsultarDatos() {
        var day = LocalDate.of(2026, 10, 26);
        assertThatThrownBy(() -> service.obtener(day, day.minusDays(1))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.obtener(day, day.plusDays(62))).isInstanceOf(BusinessException.class);
        verifyNoInteractions(cursos, matriculas, horarios, clases, tutorias, eventos);
    }
    @Test void alumnoSinMatriculaNoConsultaHorariosGlobales() {
        var alumno = TestData.usuario("ESTUDIANTE"); TestData.autenticar(alumno);
        assertThat(service.obtener(LocalDate.of(2026,10,26), LocalDate.of(2026,10,27))).isEmpty();
        verifyNoInteractions(cursos, horarios); verify(matriculas).findByEstudianteId(alumno.getId());
    }
    @Test void generaSoloOcurrenciasDentroDelCursoYConOffsetDeLima() {
        var alumno = TestData.usuario("ESTUDIANTE"); TestData.autenticar(alumno);
        var curso = TestData.curso(TestData.usuario("DOCENTE")); curso.setEstado(EstadoCurso.ACTIVO);
        curso.setFechaInicio(LocalDate.of(2026,10,26)); curso.setFechaFin(LocalDate.of(2026,11,2));
        var horario = new HorarioCurso(); horario.setId(UUID.randomUUID()); horario.setCurso(curso); horario.setDia(DayOfWeek.MONDAY); horario.setHoraInicio(LocalTime.of(19,0)); horario.setHoraFin(LocalTime.of(20,0));
        when(matriculas.findByEstudianteId(alumno.getId())).thenReturn(List.of(TestData.matricula(curso, alumno, EstadoMatricula.ACTIVA)));
        when(horarios.findByCursoIdIn(any())).thenReturn(List.of(horario));
        var result = service.obtener(LocalDate.of(2026,10,20), LocalDate.of(2026,11,15));
        assertThat(result).hasSize(2).allSatisfy(item -> {
            assertThat(item.inicio().getOffset()).isEqualTo(ZoneOffset.ofHours(-5)); assertThat(item.inicio().getHour()).isEqualTo(19);
            assertThat(item.cursoId()).isEqualTo(curso.getId());
        });
        assertThat(result.get(0).id()).isNotEqualTo(result.get(1).id());
    }
    @Test void clasePublicadaReemplazaLaOcurrenciaSemanalDelMismoHorario() {
        var docente = TestData.usuario("DOCENTE"); TestData.autenticar(docente);
        var curso = TestData.curso(docente); curso.setEstado(EstadoCurso.ACTIVO); curso.setFechaInicio(LocalDate.of(2026,10,26));
        var tarea = TestData.tarea(curso);
        var clase = new Clase(); clase.setId(UUID.randomUUID()); clase.setSemana(tarea.getSemana()); clase.setTitulo("Clase real"); clase.setFechaHora(LocalDateTime.of(2026,10,26,19,0)); clase.setDuracionMinutos(60);
        var horario = new HorarioCurso(); horario.setId(UUID.randomUUID()); horario.setCurso(curso); horario.setDia(DayOfWeek.MONDAY); horario.setHoraInicio(LocalTime.of(19,0)); horario.setHoraFin(LocalTime.of(20,0));
        when(cursos.findByDocenteId(docente.getId())).thenReturn(List.of(curso)); when(horarios.findByCursoIdIn(any())).thenReturn(List.of(horario)); when(clases.findByFechaHoraBetween(any(), any())).thenReturn(List.of(clase));
        assertThat(service.obtener(LocalDate.of(2026,10,26), LocalDate.of(2026,10,26))).singleElement().satisfies(item -> { assertThat(item.tipo()).isEqualTo("CLASE"); assertThat(item.id()).isEqualTo(clase.getId()); });
    }
}
