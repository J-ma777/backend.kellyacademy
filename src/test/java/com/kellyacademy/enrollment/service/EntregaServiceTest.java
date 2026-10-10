package com.kellyacademy.enrollment.service;

import com.kellyacademy.course.entity.Curso;
import com.kellyacademy.course.entity.Semana;
import com.kellyacademy.course.entity.Tarea;
import com.kellyacademy.course.entity.Unidad;
import com.kellyacademy.course.repository.TareaRepository;
import com.kellyacademy.enrollment.dto.request.ActualizarEntregaRequest;
import com.kellyacademy.enrollment.dto.request.CalificarEntregaRequest;
import com.kellyacademy.enrollment.dto.request.CrearEntregaRequest;
import com.kellyacademy.enrollment.dto.response.EntregaResponse;
import com.kellyacademy.enrollment.entity.Entrega;
import com.kellyacademy.enrollment.entity.Matricula;
import com.kellyacademy.enrollment.enums.EstadoEntrega;
import com.kellyacademy.enrollment.mapper.EntregaMapper;
import com.kellyacademy.enrollment.repository.EntregaRepository;
import com.kellyacademy.enrollment.repository.MatriculaRepository;
import com.kellyacademy.security.user.CustomUserDetails;
import com.kellyacademy.shared.exception.BusinessException;
import com.kellyacademy.shared.exception.ResourceNotFoundException;
import com.kellyacademy.user.entity.Rol;
import com.kellyacademy.user.entity.Usuario;
import com.kellyacademy.user.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EntregaServiceTest {

    @Mock private EntregaRepository entregaRepository;
    @Mock private TareaRepository tareaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private MatriculaRepository matriculaRepository;
    @Mock private EntregaMapper entregaMapper;
    @Mock private CalculoMatriculaService calculoMatriculaService;

    @InjectMocks private EntregaService entregaService;

    private UUID tareaId;
    private UUID estudianteId;
    private UUID cursoId;
    private UUID docenteDuenoId;
    private Tarea tarea;
    private Usuario estudiante;
    private Usuario docente;
    private Usuario docenteAjeno;

    @BeforeEach
    void setUp() {
        tareaId = UUID.randomUUID();
        estudianteId = UUID.randomUUID();
        cursoId = UUID.randomUUID();
        docenteDuenoId = UUID.randomUUID();

        Rol rolDocente = new Rol();
        rolDocente.setNombre("DOCENTE");

        docente = new Usuario();
        docente.setId(docenteDuenoId);
        docente.setRoles(Set.of(rolDocente));

        // [SLICE #21] Docente ajeno al curso de la tarea. Usado para verificar
        // que solo el docente dueno (o ADMIN) puede calificar.
        Usuario docenteAjenoUsuario = new Usuario();
        docenteAjenoUsuario.setId(UUID.randomUUID());
        docenteAjenoUsuario.setRoles(Set.of(rolDocente));
        this.docenteAjeno = docenteAjenoUsuario;

        Curso curso = new Curso();
        curso.setId(cursoId);
        curso.setDocente(docente);

        Unidad unidad = new Unidad();
        unidad.setCurso(curso);

        Semana semana = new Semana();
        semana.setUnidad(unidad);

        tarea = new Tarea();
        tarea.setId(tareaId);
        tarea.setSemana(semana);
        tarea.setFechaLimite(LocalDateTime.now().plusDays(7));

        Rol rolEstudiante = new Rol();
        rolEstudiante.setNombre("ESTUDIANTE");

        estudiante = new Usuario();
        estudiante.setId(estudianteId);
        estudiante.setRoles(Set.of(rolEstudiante));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // Simula autenticacion de un usuario en el SecurityContext.
    // Necesario porque EntregaService delega la autorizacion a SecurityUtils,
    // que revienta con IllegalStateException si no hay contexto.
    private void autenticarComo(Usuario usuario) {
        CustomUserDetails userDetails = new CustomUserDetails(usuario);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // -------- crear --------

    @Test
    void crear_cuandoTodoValido_retornaEntregaResponse() {
        autenticarComo(docente);

        CrearEntregaRequest request = new CrearEntregaRequest(
                tareaId, estudianteId, "https://example.com/archivo.pdf"
        );
        Entrega guardada = new Entrega();
        guardada.setId(UUID.randomUUID());

        when(tareaRepository.findWithSemanaCursoDocenteById(tareaId)).thenReturn(Optional.of(tarea));
        when(usuarioRepository.findWithRolesById(estudianteId)).thenReturn(Optional.of(estudiante));
        when(matriculaRepository.existsByCursoIdAndEstudianteId(cursoId, estudianteId)).thenReturn(true);
        when(entregaRepository.findByTareaIdAndEstudianteId(tareaId, estudianteId)).thenReturn(Optional.empty());
        when(entregaMapper.toEntity(request)).thenReturn(new Entrega());
        when(entregaRepository.save(any(Entrega.class))).thenReturn(guardada);
        when(entregaRepository.findWithTareaAndEstudianteById(guardada.getId()))
                .thenReturn(Optional.of(guardada));
        when(entregaMapper.toResponse(guardada)).thenReturn(mock(EntregaResponse.class));

        EntregaResponse response = entregaService.crear(request);

        assertThat(response).isNotNull();
        verify(entregaRepository).save(any(Entrega.class));
    }

    @Test
    void crear_cuandoTareaNoExiste_lanzaResourceNotFound() {
        autenticarComo(docente);

        CrearEntregaRequest request = new CrearEntregaRequest(
                tareaId, estudianteId, "https://example.com/archivo.pdf"
        );
        when(tareaRepository.findWithSemanaCursoDocenteById(tareaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> entregaService.crear(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void crear_cuandoEstudianteNoExiste_lanzaResourceNotFound() {
        autenticarComo(docente);

        CrearEntregaRequest request = new CrearEntregaRequest(
                tareaId, estudianteId, "https://example.com/archivo.pdf"
        );
        when(tareaRepository.findWithSemanaCursoDocenteById(tareaId)).thenReturn(Optional.of(tarea));
        when(usuarioRepository.findWithRolesById(estudianteId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> entregaService.crear(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void crear_cuandoEstudianteSinRolEstudiante_lanzaBusinessException() {
        autenticarComo(docente);

        estudiante.setRoles(Set.of());
        CrearEntregaRequest request = new CrearEntregaRequest(
                tareaId, estudianteId, "https://example.com/archivo.pdf"
        );
        when(tareaRepository.findWithSemanaCursoDocenteById(tareaId)).thenReturn(Optional.of(tarea));
        when(usuarioRepository.findWithRolesById(estudianteId)).thenReturn(Optional.of(estudiante));

        assertThatThrownBy(() -> entregaService.crear(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ESTUDIANTE");
    }

    @Test
    void crear_cuandoEstudianteNoMatriculado_lanzaBusinessException() {
        autenticarComo(docente);

        CrearEntregaRequest request = new CrearEntregaRequest(
                tareaId, estudianteId, "https://example.com/archivo.pdf"
        );
        when(tareaRepository.findWithSemanaCursoDocenteById(tareaId)).thenReturn(Optional.of(tarea));
        when(usuarioRepository.findWithRolesById(estudianteId)).thenReturn(Optional.of(estudiante));
        when(matriculaRepository.existsByCursoIdAndEstudianteId(cursoId, estudianteId)).thenReturn(false);

        assertThatThrownBy(() -> entregaService.crear(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("matriculado");
    }

    @Test
    void crear_cuandoEntregaDuplicada_lanzaBusinessException() {
        autenticarComo(docente);

        CrearEntregaRequest request = new CrearEntregaRequest(
                tareaId, estudianteId, "https://example.com/archivo.pdf"
        );
        when(tareaRepository.findWithSemanaCursoDocenteById(tareaId)).thenReturn(Optional.of(tarea));
        when(usuarioRepository.findWithRolesById(estudianteId)).thenReturn(Optional.of(estudiante));
        when(matriculaRepository.existsByCursoIdAndEstudianteId(cursoId, estudianteId)).thenReturn(true);
        when(entregaRepository.findByTareaIdAndEstudianteId(tareaId, estudianteId))
                .thenReturn(Optional.of(new Entrega()));

        assertThatThrownBy(() -> entregaService.crear(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Ya existe una entrega");
    }

    // -------- actualizar --------

    @Test
    void actualizar_cuandoEntregaCalificada_lanzaBusinessException() {
        // Autenticamos como el estudiante dueno: es quien tiene permitido actualizar.
        autenticarComo(estudiante);

        ActualizarEntregaRequest request = new ActualizarEntregaRequest("https://example.com/nuevo.pdf");
        Entrega entrega = new Entrega();
        entrega.setEstado(EstadoEntrega.CALIFICADA);
        entrega.setEstudiante(estudiante);
        entrega.setTarea(tarea);

        when(entregaRepository.findWithTareaAndEstudianteById(any())).thenReturn(Optional.of(entrega));

        assertThatThrownBy(() -> entregaService.actualizar(UUID.randomUUID(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("calificada");
    }

    @Test
    void eliminar_cuandoCalificada_lanzaBusinessException() {
        UUID entregaId = UUID.randomUUID();
        Entrega entrega = new Entrega();
        entrega.setId(entregaId);
        entrega.setEstado(EstadoEntrega.CALIFICADA);

        when(entregaRepository.findById(entregaId)).thenReturn(Optional.of(entrega));

        assertThatThrownBy(() -> entregaService.eliminar(entregaId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("No se puede eliminar una entrega calificada");

        verify(entregaRepository, never()).delete(any(Entrega.class));
    }

    @Test
    void eliminar_cuandoPendiente_ok() {
        UUID entregaId = UUID.randomUUID();
        Entrega entrega = new Entrega();
        entrega.setId(entregaId);
        entrega.setEstado(EstadoEntrega.PENDIENTE);

        when(entregaRepository.findById(entregaId)).thenReturn(Optional.of(entrega));

        entregaService.eliminar(entregaId);

        verify(entregaRepository).delete(entrega);
    }

    // -------- calificar (SLICE #21) --------

    @Test
    void calificar_cuandoDocenteDueno_retornaEntregaCalificada() {
        autenticarComo(docente);

        Entrega entrega = new Entrega();
        entrega.setId(UUID.randomUUID());
        entrega.setTarea(tarea);
        entrega.setEstudiante(estudiante);
        entrega.setEstado(EstadoEntrega.PENDIENTE);
        entrega.setUrlArchivo("https://example.com/archivo.pdf");

        CalificarEntregaRequest request = new CalificarEntregaRequest(
                new BigDecimal("85.50"), "Buen trabajo"
        );

        Matricula matriculaMock = new Matricula();
        matriculaMock.setId(UUID.randomUUID());

        when(entregaRepository.findWithTareaAndEstudianteById(entrega.getId()))
                .thenReturn(Optional.of(entrega));
        when(usuarioRepository.findById(docenteDuenoId)).thenReturn(Optional.of(docente));
        when(matriculaRepository.findByCursoIdAndEstudianteId(cursoId, estudianteId))
                .thenReturn(Optional.of(matriculaMock));
        when(entregaMapper.toResponse(entrega)).thenReturn(mock(EntregaResponse.class));

        EntregaResponse response = entregaService.calificar(entrega.getId(), request);

        assertThat(response).isNotNull();
        assertThat(entrega.getNota()).isEqualByComparingTo("85.50");
        assertThat(entrega.getRetroalimentacion()).isEqualTo("Buen trabajo");
        assertThat(entrega.getEstado()).isEqualTo(EstadoEntrega.CALIFICADA);
        verify(calculoMatriculaService).recalcular(matriculaMock.getId());
    }

    @Test
    void calificar_cuandoDocenteAjeno_lanzaAccessDenied() {
        autenticarComo(docenteAjeno); // ver helper abajo

        Entrega entrega = new Entrega();
        entrega.setId(UUID.randomUUID());
        entrega.setTarea(tarea);
        entrega.setEstudiante(estudiante);
        entrega.setEstado(EstadoEntrega.PENDIENTE);

        CalificarEntregaRequest request = new CalificarEntregaRequest(
                new BigDecimal("80.00"), null
        );

        when(entregaRepository.findWithTareaAndEstudianteById(entrega.getId()))
                .thenReturn(Optional.of(entrega));

        assertThatThrownBy(() -> entregaService.calificar(entrega.getId(), request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void calificar_cuandoEntregaNoExiste_lanzaResourceNotFound() {
        autenticarComo(docente);

        UUID id = UUID.randomUUID();
        CalificarEntregaRequest request = new CalificarEntregaRequest(
                new BigDecimal("80.00"), null
        );

        when(entregaRepository.findWithTareaAndEstudianteById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> entregaService.calificar(id, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void calificar_cuandoNotaExcedePuntajeMaximo_lanzaBusinessException() {
        autenticarComo(docente);

        // puntajeMaximo por defecto en Tarea es 100
        Entrega entrega = new Entrega();
        entrega.setId(UUID.randomUUID());
        entrega.setTarea(tarea);
        entrega.setEstudiante(estudiante);
        entrega.setEstado(EstadoEntrega.PENDIENTE);

        CalificarEntregaRequest request = new CalificarEntregaRequest(
                new BigDecimal("150.00"), null
        );

        when(entregaRepository.findWithTareaAndEstudianteById(entrega.getId()))
                .thenReturn(Optional.of(entrega));

        assertThatThrownBy(() -> entregaService.calificar(entrega.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("excede");
    }

    @Test
    void calificar_cuandoNotaNegativa_lanzaBusinessException() {
        autenticarComo(docente);

        Entrega entrega = new Entrega();
        entrega.setId(UUID.randomUUID());
        entrega.setTarea(tarea);
        entrega.setEstudiante(estudiante);
        entrega.setEstado(EstadoEntrega.PENDIENTE);

        CalificarEntregaRequest request = new CalificarEntregaRequest(
                new BigDecimal("-1.00"), null
        );

        when(entregaRepository.findWithTareaAndEstudianteById(entrega.getId()))
                .thenReturn(Optional.of(entrega));

        assertThatThrownBy(() -> entregaService.calificar(entrega.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("negativa");
    }

    @Test
    void calificar_cuandoYaCalificada_permiteRecalificar() {
        autenticarComo(docente);

        Entrega entrega = new Entrega();
        entrega.setId(UUID.randomUUID());
        entrega.setTarea(tarea);
        entrega.setEstudiante(estudiante);
        entrega.setEstado(EstadoEntrega.CALIFICADA);
        entrega.setNota(new BigDecimal("70.00"));
        entrega.setRetroalimentacion("Anterior");

        CalificarEntregaRequest request = new CalificarEntregaRequest(
                new BigDecimal("90.00"), "Correccion"
        );

        when(entregaRepository.findWithTareaAndEstudianteById(entrega.getId()))
                .thenReturn(Optional.of(entrega));
        when(usuarioRepository.findById(docenteDuenoId)).thenReturn(Optional.of(docente));
        when(entregaMapper.toResponse(entrega)).thenReturn(mock(EntregaResponse.class));

        entregaService.calificar(entrega.getId(), request);

        assertThat(entrega.getNota()).isEqualByComparingTo("90.00");
        assertThat(entrega.getRetroalimentacion()).isEqualTo("Correccion");
        assertThat(entrega.getEstado()).isEqualTo(EstadoEntrega.CALIFICADA);
    }

    @Test
    void crear_cuandoUrlArchivoEsNull_creaEntregaSinUrl() {
        autenticarComo(docente);

        CrearEntregaRequest request = new CrearEntregaRequest(
                tareaId, estudianteId, null
        );
        Entrega guardada = new Entrega();
        guardada.setId(UUID.randomUUID());

        when(tareaRepository.findWithSemanaCursoDocenteById(tareaId)).thenReturn(Optional.of(tarea));
        when(usuarioRepository.findWithRolesById(estudianteId)).thenReturn(Optional.of(estudiante));
        when(matriculaRepository.existsByCursoIdAndEstudianteId(cursoId, estudianteId)).thenReturn(true);
        when(entregaRepository.findByTareaIdAndEstudianteId(tareaId, estudianteId)).thenReturn(Optional.empty());
        when(entregaMapper.toEntity(request)).thenReturn(new Entrega());
        when(entregaRepository.save(any(Entrega.class))).thenReturn(guardada);
        when(entregaRepository.findWithTareaAndEstudianteById(guardada.getId()))
                .thenReturn(Optional.of(guardada));
        when(entregaMapper.toResponse(guardada)).thenReturn(mock(EntregaResponse.class));

        EntregaResponse response = entregaService.crear(request);

        assertThat(response).isNotNull();
        verify(entregaRepository).save(any(Entrega.class));
    }

    @Test
    void calificar_seteaCalificadoAtYCalificadoPor() {
        autenticarComo(docente);

        Entrega entrega = new Entrega();
        entrega.setId(UUID.randomUUID());
        entrega.setTarea(tarea);
        entrega.setEstudiante(estudiante);
        entrega.setEstado(EstadoEntrega.PENDIENTE);

        CalificarEntregaRequest request = new CalificarEntregaRequest(
                new BigDecimal("85.50"), "Buen trabajo"
        );

        Matricula matriculaMock = new Matricula();
        matriculaMock.setId(UUID.randomUUID());

        when(entregaRepository.findWithTareaAndEstudianteById(entrega.getId()))
                .thenReturn(Optional.of(entrega));
        when(usuarioRepository.findById(docenteDuenoId)).thenReturn(Optional.of(docente));
        when(matriculaRepository.findByCursoIdAndEstudianteId(cursoId, estudianteId))
                .thenReturn(Optional.of(matriculaMock));
        when(entregaMapper.toResponse(entrega)).thenReturn(mock(EntregaResponse.class));

        entregaService.calificar(entrega.getId(), request);

        assertThat(entrega.getCalificadoAt()).isNotNull();
        assertThat(entrega.getCalificadoPor()).isEqualTo(docente);
    }

    @Test
    void recalificar_sobrescribeCalificadoAtYCalificadoPor() {
        autenticarComo(docente);

        // Entrega ya calificada previamente por otro docente.
        Usuario calificadorOriginal = new Usuario();
        calificadorOriginal.setId(UUID.randomUUID());

        Entrega entrega = new Entrega();
        entrega.setId(UUID.randomUUID());
        entrega.setTarea(tarea);
        entrega.setEstudiante(estudiante);
        entrega.setEstado(EstadoEntrega.CALIFICADA);
        entrega.setNota(new BigDecimal("70.00"));
        entrega.setCalificadoAt(LocalDateTime.now().minusDays(3));
        entrega.setCalificadoPor(calificadorOriginal);

        LocalDateTime calificadoAtPrevio = entrega.getCalificadoAt();

        CalificarEntregaRequest request = new CalificarEntregaRequest(
                new BigDecimal("90.00"), "Correccion"
        );

        Matricula matriculaMock = new Matricula();
        matriculaMock.setId(UUID.randomUUID());

        when(entregaRepository.findWithTareaAndEstudianteById(entrega.getId()))
                .thenReturn(Optional.of(entrega));
        when(usuarioRepository.findById(docenteDuenoId)).thenReturn(Optional.of(docente));
        when(matriculaRepository.findByCursoIdAndEstudianteId(cursoId, estudianteId))
                .thenReturn(Optional.of(matriculaMock));
        when(entregaMapper.toResponse(entrega)).thenReturn(mock(EntregaResponse.class));

        entregaService.calificar(entrega.getId(), request);

        assertThat(entrega.getCalificadoAt()).isAfter(calificadoAtPrevio);
        assertThat(entrega.getCalificadoPor()).isEqualTo(docente);
        assertThat(entrega.getNota()).isEqualByComparingTo("90.00");
    }
}