package com.kellyacademy.user.service;

import com.kellyacademy.user.dto.alumno.AlumnoDtos.*;
import com.kellyacademy.user.entity.Usuario;
import com.kellyacademy.user.enums.EstadoUsuario;
import com.kellyacademy.user.repository.*;
import com.kellyacademy.course.entity.Curso;
import com.kellyacademy.course.enums.EstadoCurso;
import com.kellyacademy.course.repository.CursoRepository;
import com.kellyacademy.enrollment.entity.Matricula;
import com.kellyacademy.enrollment.enums.EstadoMatricula;
import com.kellyacademy.enrollment.repository.MatriculaRepository;
import com.kellyacademy.shared.exception.*;
import com.kellyacademy.support.TestData;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class AlumnoDocenteServiceTest {
    @Mock UsuarioRepository usuarios;
    @Mock RolRepository roles;
    @Mock CursoRepository cursos;
    @Mock MatriculaRepository matriculas;
    @Mock PasswordEncoder passwords;
    @InjectMocks AlumnoDocenteService service;
    private final Usuario alumno = TestData.usuario("ESTUDIANTE");
    @Test void altaNormalizaNombreCorreoYGuardaSoloPasswordCodificado() {
        when(roles.findByNombre("ESTUDIANTE")).thenReturn(Optional.of(alumno.getRoles().iterator().next()));
        when(passwords.encode("PasswordTest123")).thenReturn("hash-cifrado-de-prueba");
        when(usuarios.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        var result = service.crear(new Crear("  Ana   María Pérez  ", "Ana.Perez", "999159716", "PasswordTest123"));
        var captor = ArgumentCaptor.forClass(Usuario.class); verify(usuarios).saveAndFlush(captor.capture());
        assertThat(result.correoElectronico()).isEqualTo("ana.perez@kellyacademy.com");
        assertThat(result.nombreCompleto()).isEqualTo("Ana María Pérez");
        assertThat(captor.getValue().getContrasena()).isEqualTo("hash-cifrado-de-prueba");
        assertThat(captor.getValue().getRoles()).extracting("nombre").containsExactly("ESTUDIANTE");
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
    }
    @Test void correoDuplicadoNoCifraNiInsertaOtraCuenta() {
        when(usuarios.existsByCorreoElectronicoIgnoreCase("ana@kellyacademy.com")).thenReturn(true);
        assertThatThrownBy(() -> service.crear(new Crear("Ana Pérez", "ana", "999159716", "PasswordTest123"))).isInstanceOf(CorreoYaRegistradoException.class);
        verifyNoInteractions(passwords); verify(usuarios, never()).saveAndFlush(any());
    }
    @Test void validaLimiteBcryptEnBytesYNoSoloCaracteres() {
        assertThatThrownBy(() -> service.crear(new Crear("Ana Pérez", "ana", "999159716", "á".repeat(37)))).isInstanceOf(BusinessException.class).hasMessageContaining("72 bytes");
        verifyNoInteractions(passwords); verify(usuarios, never()).saveAndFlush(any());
    }
    @Test void cuentaMixtaODocenteNuncaSeEliminaDesdeModuloAlumnos() {
        var docente = TestData.usuario("DOCENTE");
        when(usuarios.findByIdForUpdate(docente.getId())).thenReturn(Optional.of(docente));
        assertThatThrownBy(() -> service.eliminar(docente.getId())).isInstanceOf(AccessDeniedException.class);
        verify(usuarios, never()).delete(any());
    }
    @Test void suspenderCuentaUsaElEstadoQueBloqueaAutenticacion() {
        when(usuarios.findByIdForUpdate(alumno.getId())).thenReturn(Optional.of(alumno));
        var result = service.cambiarEstado(alumno.getId(), EstadoCuenta.SUSPENDIDO);
        assertThat(alumno.getEstado()).isEqualTo(EstadoUsuario.BLOQUEADO); assertThat(result.estado()).isEqualTo(EstadoCuenta.SUSPENDIDO);
    }
    private Curso curso() {
        var curso = TestData.curso(TestData.usuario("DOCENTE")); curso.setEstado(EstadoCurso.ACTIVO);
        when(usuarios.findByIdForUpdate(alumno.getId())).thenReturn(Optional.of(alumno));
        when(cursos.findByIdForUpdate(curso.getId())).thenReturn(Optional.of(curso)); return curso;
    }
    @Test void cursoLlenoNoRecibeUnaMatriculaAdicional() {
        var curso = curso(); when(matriculas.countByCursoIdAndEstadoIn(eq(curso.getId()), any())).thenReturn(20L);
        assertThatThrownBy(() -> service.inscribir(alumno.getId(), curso.getId())).isInstanceOf(BusinessException.class).hasMessageContaining("cupos");
        verify(matriculas, never()).save(any());
    }
    @Test void inscripcionYaActivaEsIdempotenteYConservaHistorial() {
        var curso = curso(); var actual = TestData.matricula(curso, alumno, EstadoMatricula.ACTIVA);
        when(matriculas.findByCursoIdAndEstudianteId(curso.getId(), alumno.getId())).thenReturn(Optional.of(actual));
        service.inscribir(alumno.getId(), curso.getId());
        verify(matriculas, never()).save(any()); verify(matriculas, never()).countByCursoIdAndEstadoIn(any(), any());
    }
    @Test void reinscribirReactivaLaMismaMatriculaAbandonada() {
        var curso = curso(); var actual = TestData.matricula(curso, alumno, EstadoMatricula.ABANDONADA);
        when(matriculas.findByCursoIdAndEstudianteId(curso.getId(), alumno.getId())).thenReturn(Optional.of(actual));
        service.inscribir(alumno.getId(), curso.getId());
        verify(matriculas).save(actual); assertThat(actual.getEstado()).isEqualTo(EstadoMatricula.ACTIVA);
    }
    @Test void retirarConservaLaMatriculaComoAbandonada() {
        var curso = TestData.curso(TestData.usuario("DOCENTE")); var actual = TestData.matricula(curso, alumno, EstadoMatricula.ACTIVA);
        when(usuarios.findByIdForUpdate(alumno.getId())).thenReturn(Optional.of(alumno)); when(cursos.existsById(curso.getId())).thenReturn(true);
        when(matriculas.findByCursoIdAndEstudianteId(curso.getId(), alumno.getId())).thenReturn(Optional.of(actual));
        service.retirar(alumno.getId(), curso.getId()); assertThat(actual.getEstado()).isEqualTo(EstadoMatricula.ABANDONADA);
        verify(matriculas, never()).delete(any(Matricula.class));
    }
}
