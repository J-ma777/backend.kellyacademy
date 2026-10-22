package com.kellyacademy.course.service;

import com.kellyacademy.course.entity.*;
import com.kellyacademy.course.repository.*;
import com.kellyacademy.enrollment.enums.EstadoMatricula;
import com.kellyacademy.enrollment.repository.MatriculaRepository;
import com.kellyacademy.shared.exception.ResourceNotFoundException;
import com.kellyacademy.support.TestData;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccesoCursoServiceTest {
    @Mock CursoRepository cursos;
    @Mock MatriculaRepository matriculas;
    @Mock SemanaRepository semanas;
    @InjectMocks AccesoCursoService acceso;
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    @Test void propietarioYAdministradorPuedenGestionarSinMatricula() {
        var docente = TestData.usuario("DOCENTE"); var curso = TestData.curso(docente);
        TestData.autenticar(docente); acceso.validarGestor(curso); acceso.validarLectura(curso);
        TestData.autenticar(TestData.usuario("ADMINISTRADOR")); acceso.validarGestor(curso); acceso.validarLectura(curso);
        verifyNoInteractions(matriculas);
    }
    @Test void docenteAjenoNoPuedeGestionarNiLeerSinMatricula() {
        TestData.autenticar(TestData.usuario("DOCENTE")); var curso = TestData.curso(TestData.usuario("DOCENTE"));
        assertThatThrownBy(() -> acceso.validarGestor(curso)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> acceso.validarLectura(curso)).isInstanceOf(AccessDeniedException.class);
    }
    @ParameterizedTest @EnumSource(EstadoMatricula.class)
    void permisosDeLecturaYEntregaRespetanElEstadoDeLaMatricula(EstadoMatricula estado) {
        var alumno = TestData.usuario("ESTUDIANTE"); TestData.autenticar(alumno);
        var curso = TestData.curso(TestData.usuario("DOCENTE"));
        when(matriculas.findByCursoIdAndEstudianteId(curso.getId(), alumno.getId())).thenReturn(Optional.of(TestData.matricula(curso, alumno, estado)));
        if (estado == EstadoMatricula.ABANDONADA) assertThatThrownBy(() -> acceso.validarLectura(curso)).isInstanceOf(AccessDeniedException.class);
        else assertThatCode(() -> acceso.validarLectura(curso)).doesNotThrowAnyException();
        if (estado == EstadoMatricula.ACTIVA || estado == EstadoMatricula.RIESGO) assertThatCode(() -> acceso.validarEstudianteActivo(curso)).doesNotThrowAnyException();
        else assertThatThrownBy(() -> acceso.validarEstudianteActivo(curso)).isInstanceOf(AccessDeniedException.class);
    }
    @Test void estudianteSoloVeContenidoPublicadoYVisibleDentroDeSuCurso() {
        var alumno = TestData.usuario("ESTUDIANTE"); TestData.autenticar(alumno);
        var curso = TestData.curso(TestData.usuario("DOCENTE")); var tarea = TestData.tarea(curso);
        when(matriculas.findByCursoIdAndEstudianteId(curso.getId(), alumno.getId())).thenReturn(Optional.of(TestData.matricula(curso, alumno, EstadoMatricula.ACTIVA)));
        acceso.validarContenido(tarea);
        tarea.setPublicado(false); assertThatThrownBy(() -> acceso.validarContenido(tarea)).isInstanceOf(AccessDeniedException.class);
        tarea.setPublicado(true); tarea.setVisible(false); assertThatThrownBy(() -> acceso.validarContenido(tarea)).isInstanceOf(AccessDeniedException.class);
    }
    @Test void recursoInexistenteNoSeConfundeConFaltaDePermisos() {
        assertThatThrownBy(() -> acceso.obtener(UUID.randomUUID())).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> acceso.validarSemana(UUID.randomUUID())).isInstanceOf(ResourceNotFoundException.class);
    }
}
