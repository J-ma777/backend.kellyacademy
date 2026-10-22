package com.kellyacademy.course.service;

import com.kellyacademy.course.entity.*;
import com.kellyacademy.course.dto.request.HorarioCursoRequest;
import com.kellyacademy.course.repository.HorarioCursoRepository;
import com.kellyacademy.user.repository.UsuarioRepository;
import com.kellyacademy.shared.exception.BusinessException;
import com.kellyacademy.support.TestData;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HorarioCursoServiceTest {
    @Mock HorarioCursoRepository horarios;
    @Mock UsuarioRepository usuarios;
    @Mock AccesoCursoService acceso;
    @InjectMocks HorarioCursoService service;
    private Curso curso;
    @BeforeEach void setup() { curso = TestData.curso(TestData.usuario("DOCENTE")); curso.setFechaInicio(LocalDate.of(2026, 10, 26)); }
    private HorarioCursoRequest bloque(DayOfWeek dia, int desde, int hasta) { return new HorarioCursoRequest(dia, LocalTime.of(desde, 0), LocalTime.of(hasta, 0)); }
    private void ocupados(List<HorarioCurso> existentes) {
        when(usuarios.findByIdForUpdate(curso.getDocente().getId())).thenReturn(Optional.of(curso.getDocente()));
        when(horarios.findActivosDelDocente(curso.getDocente().getId())).thenReturn(existentes);
    }
    @Test void rechazaFechasIncompletasInvertidasYHorarioVacioAntesDeReservar() {
        curso.setFechaInicio(null);
        assertThatThrownBy(() -> service.aplicar(curso, List.of(bloque(DayOfWeek.MONDAY, 9, 10)))).isInstanceOf(BusinessException.class).hasMessageContaining("inicio");
        curso.setFechaInicio(LocalDate.of(2026, 10, 26)); curso.setFechaFin(curso.getFechaInicio().minusDays(1));
        assertThatThrownBy(() -> service.aplicar(curso, List.of(bloque(DayOfWeek.MONDAY, 9, 10)))).isInstanceOf(BusinessException.class).hasMessageContaining("fin");
        curso.setFechaFin(null);
        assertThatThrownBy(() -> service.aplicar(curso, List.of())).isInstanceOf(BusinessException.class).hasMessageContaining("franja");
        verifyNoInteractions(usuarios, horarios);
    }
    @Test void rechazaHorasInvertidasSegundosYFranjasSuperpuestas() {
        for (var blocks : List.of(List.of(bloque(DayOfWeek.MONDAY, 10, 9)), List.of(new HorarioCursoRequest(DayOfWeek.MONDAY, LocalTime.of(9,0,1), LocalTime.of(10,0))), List.of(bloque(DayOfWeek.MONDAY, 9, 11), bloque(DayOfWeek.MONDAY, 10, 12)))) {
            assertThatThrownBy(() -> service.aplicar(curso, blocks)).isInstanceOf(BusinessException.class);
        }
        verifyNoInteractions(usuarios, horarios);
    }
    @Test void ordenaFranjasYPermiteClasesAdyacentesSinDuplicarlas() {
        ocupados(List.of());
        service.aplicar(curso, List.of(bloque(DayOfWeek.FRIDAY, 9, 10), bloque(DayOfWeek.MONDAY, 10, 11), bloque(DayOfWeek.MONDAY, 9, 10)));
        assertThat(curso.getHorarios()).hasSize(3);
        assertThat(curso.getHorario()).isEqualTo("Lun 09:00–10:00 · Lun 10:00–11:00 · Vie 09:00–10:00");
        assertThat(curso.getHorarios()).allSatisfy(h -> assertThat(h.getCurso()).isSameAs(curso));
    }
    private HorarioCurso conflicto(LocalDate fin) {
        var otro = TestData.curso(curso.getDocente()); otro.setFechaInicio(curso.getFechaInicio().minusDays(7)); otro.setFechaFin(fin);
        var horario = new HorarioCurso(); horario.setCurso(otro); horario.setDia(DayOfWeek.MONDAY); horario.setHoraInicio(LocalTime.of(9,0)); horario.setHoraFin(LocalTime.of(11,0)); return horario;
    }
    @Test void rechazaCruceRealConOtroCursoDelMismoDocente() {
        ocupados(List.of(conflicto(null)));
        assertThatThrownBy(() -> service.aplicar(curso, List.of(bloque(DayOfWeek.MONDAY, 10, 12)))).isInstanceOf(BusinessException.class).hasMessageContaining("ya tiene una clase");
        assertThat(curso.getHorarios()).isEmpty();
    }
    @Test void mismaFranjaEnPeriodosSeparadosNoEsConflicto() {
        ocupados(List.of(conflicto(curso.getFechaInicio().minusDays(1))));
        service.aplicar(curso, List.of(bloque(DayOfWeek.MONDAY, 10, 12)));
        assertThat(curso.getHorarios()).hasSize(1);
    }
    @Test void alEditarIgnoraLaReservaPropiaYReemplazaLasFranjas() {
        var actual = conflicto(null); actual.setCurso(curso); curso.getHorarios().add(actual); ocupados(List.of(actual));
        service.aplicar(curso, List.of(bloque(DayOfWeek.MONDAY, 10, 12)));
        assertThat(curso.getHorarios()).singleElement().satisfies(h -> assertThat(h.getHoraInicio()).isEqualTo(LocalTime.of(10,0)));
    }
}
