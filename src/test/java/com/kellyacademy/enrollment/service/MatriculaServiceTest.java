package com.kellyacademy.enrollment.service;

import com.kellyacademy.course.entity.Curso;
import com.kellyacademy.course.enums.EstadoCurso;
import com.kellyacademy.course.repository.CursoRepository;
import com.kellyacademy.enrollment.dto.request.CrearMatriculaRequest;
import com.kellyacademy.enrollment.dto.response.MatriculaResponse;
import com.kellyacademy.enrollment.entity.Matricula;
import com.kellyacademy.enrollment.enums.EstadoMatricula;
import com.kellyacademy.enrollment.mapper.MatriculaMapper;
import com.kellyacademy.enrollment.repository.MatriculaRepository;
import com.kellyacademy.shared.exception.BusinessException;
import com.kellyacademy.shared.exception.ResourceNotFoundException;
import com.kellyacademy.user.entity.Rol;
import com.kellyacademy.user.entity.Usuario;
import com.kellyacademy.user.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatriculaServiceTest {

    @Mock private MatriculaRepository matriculaRepository;
    @Mock private CursoRepository cursoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private MatriculaMapper matriculaMapper;

    @InjectMocks private MatriculaService matriculaService;

    private UUID cursoId;
    private UUID estudianteId;
    private Curso curso;
    private Usuario estudiante;

    @BeforeEach
    void setUp() {
        cursoId = UUID.randomUUID();
        estudianteId = UUID.randomUUID();

        curso = new Curso();
        curso.setId(cursoId);
        curso.setEstado(EstadoCurso.ACTIVO);
        curso.setCapacidadMaxima(30);

        Rol rolEstudiante = new Rol();
        rolEstudiante.setNombre("ESTUDIANTE");

        estudiante = new Usuario();
        estudiante.setId(estudianteId);
        estudiante.setRoles(Set.of(rolEstudiante));
    }

    @Test
    void crear_cuandoTodoValido_retornaMatriculaResponse() {
        CrearMatriculaRequest request = new CrearMatriculaRequest(cursoId, estudianteId);
        Matricula matriculaGuardada = new Matricula();
        matriculaGuardada.setId(UUID.randomUUID());

        when(cursoRepository.findWithDocenteById(cursoId)).thenReturn(Optional.of(curso));
        when(usuarioRepository.findWithRolesById(estudianteId)).thenReturn(Optional.of(estudiante));
        when(matriculaRepository.countByCursoIdAndEstado(cursoId, EstadoMatricula.ACTIVA)).thenReturn(0L);
        when(matriculaRepository.existsByCursoIdAndEstudianteId(cursoId, estudianteId)).thenReturn(false);
        when(matriculaMapper.toEntity(request)).thenReturn(new Matricula());
        when(matriculaRepository.save(any(Matricula.class))).thenReturn(matriculaGuardada);
        when(matriculaRepository.findWithCursoAndEstudianteById(matriculaGuardada.getId()))
                .thenReturn(Optional.of(matriculaGuardada));
        when(matriculaMapper.toResponse(matriculaGuardada)).thenReturn(mock(MatriculaResponse.class));

        MatriculaResponse response = matriculaService.crear(request);

        assertThat(response).isNotNull();
        verify(matriculaRepository).save(any(Matricula.class));
    }

    @Test
    void crear_cuandoCursoNoExiste_lanzaResourceNotFound() {
        CrearMatriculaRequest request = new CrearMatriculaRequest(cursoId, estudianteId);
        when(cursoRepository.findWithDocenteById(cursoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matriculaService.crear(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void crear_cuandoEstudianteNoExiste_lanzaResourceNotFound() {
        CrearMatriculaRequest request = new CrearMatriculaRequest(cursoId, estudianteId);
        when(cursoRepository.findWithDocenteById(cursoId)).thenReturn(Optional.of(curso));
        when(usuarioRepository.findWithRolesById(estudianteId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matriculaService.crear(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void crear_cuandoEstudianteSinRolEstudiante_lanzaBusinessException() {
        estudiante.setRoles(Set.of());
        CrearMatriculaRequest request = new CrearMatriculaRequest(cursoId, estudianteId);
        when(cursoRepository.findWithDocenteById(cursoId)).thenReturn(Optional.of(curso));
        when(usuarioRepository.findWithRolesById(estudianteId)).thenReturn(Optional.of(estudiante));

        assertThatThrownBy(() -> matriculaService.crear(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ESTUDIANTE");
    }

    @Test
    void crear_cuandoCursoNoActivo_lanzaBusinessException() {
        curso.setEstado(EstadoCurso.BORRADOR);
        CrearMatriculaRequest request = new CrearMatriculaRequest(cursoId, estudianteId);
        when(cursoRepository.findWithDocenteById(cursoId)).thenReturn(Optional.of(curso));
        when(usuarioRepository.findWithRolesById(estudianteId)).thenReturn(Optional.of(estudiante));

        assertThatThrownBy(() -> matriculaService.crear(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("activo");
    }

    @Test
    void crear_cuandoCursoSinCupo_lanzaBusinessException() {
        curso.setCapacidadMaxima(1);
        CrearMatriculaRequest request = new CrearMatriculaRequest(cursoId, estudianteId);
        when(cursoRepository.findWithDocenteById(cursoId)).thenReturn(Optional.of(curso));
        when(usuarioRepository.findWithRolesById(estudianteId)).thenReturn(Optional.of(estudiante));
        when(matriculaRepository.countByCursoIdAndEstado(cursoId, EstadoMatricula.ACTIVA)).thenReturn(1L);

        assertThatThrownBy(() -> matriculaService.crear(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("capacidad");
    }

    @Test
    void crear_cuandoMatriculaDuplicada_lanzaBusinessException() {
        CrearMatriculaRequest request = new CrearMatriculaRequest(cursoId, estudianteId);
        when(cursoRepository.findWithDocenteById(cursoId)).thenReturn(Optional.of(curso));
        when(usuarioRepository.findWithRolesById(estudianteId)).thenReturn(Optional.of(estudiante));
        when(matriculaRepository.countByCursoIdAndEstado(cursoId, EstadoMatricula.ACTIVA)).thenReturn(0L);
        when(matriculaRepository.existsByCursoIdAndEstudianteId(cursoId, estudianteId)).thenReturn(true);

        assertThatThrownBy(() -> matriculaService.crear(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("matriculado");
    }
}