package com.kellyacademy.communication.service;

import com.kellyacademy.communication.dto.request.CrearConversacionRequest;
import com.kellyacademy.communication.dto.response.ConversacionResponse;
import com.kellyacademy.communication.entity.Conversacion;
import com.kellyacademy.communication.mapper.ConversacionMapper;
import com.kellyacademy.communication.repository.ConversacionRepository;
import com.kellyacademy.communication.repository.MensajeRepository;
import com.kellyacademy.course.entity.Curso;
import com.kellyacademy.course.repository.CursoRepository;
import com.kellyacademy.enrollment.enums.EstadoMatricula;
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

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversacionServiceTest {

    @Mock private ConversacionRepository conversacionRepository;
    @Mock private MensajeRepository mensajeRepository;
    @Mock private CursoRepository cursoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private MatriculaRepository matriculaRepository;
    @Mock private ConversacionMapper conversacionMapper;

    @InjectMocks private ConversacionService conversacionService;

    private UUID autenticadoId;
    private UUID otroId;
    private UUID cursoId;
    private UUID conversacionId;
    private Usuario autenticado;
    private Usuario otro;
    private Usuario admin;
    private Curso curso;

    @BeforeEach
    void setUp() {
        autenticadoId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        otroId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        cursoId = UUID.randomUUID();
        conversacionId = UUID.randomUUID();

        Rol rolEstudiante = new Rol();
        rolEstudiante.setNombre("ESTUDIANTE");

        autenticado = new Usuario();
        autenticado.setId(autenticadoId);
        autenticado.setRoles(Set.of(rolEstudiante));

        otro = new Usuario();
        otro.setId(otroId);
        otro.setRoles(Set.of(rolEstudiante));

        Rol rolAdmin = new Rol();
        rolAdmin.setNombre("ADMINISTRADOR");
        admin = new Usuario();
        admin.setId(UUID.randomUUID());
        admin.setRoles(Set.of(rolAdmin));

        curso = new Curso();
        curso.setId(cursoId);
        curso.setDocente(admin);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(Usuario u) {
        CustomUserDetails details = new CustomUserDetails(u);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                details, null, details.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // -------- crear --------

    @Test
    void crear_cuandoAutoconversacion_lanzaBusinessException() {
        autenticarComo(autenticado);
        CrearConversacionRequest request = new CrearConversacionRequest(null, autenticadoId, "Asunto");

        assertThatThrownBy(() -> conversacionService.crear(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("consigo mismo");
    }

    @Test
    void crear_cuandoOtroNoExiste_lanzaResourceNotFound() {
        autenticarComo(autenticado);
        CrearConversacionRequest request = new CrearConversacionRequest(null, otroId, "Asunto");

        when(usuarioRepository.findById(autenticadoId)).thenReturn(Optional.of(autenticado));
        when(usuarioRepository.findById(otroId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> conversacionService.crear(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void crear_cuandoSinCursoYNoExiste_creaNueva() {
        autenticarComo(autenticado);
        CrearConversacionRequest request = new CrearConversacionRequest(null, otroId, "Asunto");

        Conversacion guardada = new Conversacion();
        guardada.setId(conversacionId);
        guardada.setParticipante1(autenticado);
        guardada.setParticipante2(otro);

        when(usuarioRepository.findById(autenticadoId)).thenReturn(Optional.of(autenticado));
        when(usuarioRepository.findById(otroId)).thenReturn(Optional.of(otro));
        when(conversacionRepository.findByCursoIsNullAndParticipante1IdAndParticipante2Id(autenticadoId, otroId))
                .thenReturn(Optional.empty());
        when(conversacionMapper.toEntity(request)).thenReturn(new Conversacion());
        when(conversacionRepository.save(any(Conversacion.class))).thenReturn(guardada);
        when(conversacionMapper.toResponse(guardada, 0L)).thenReturn(mock(ConversacionResponse.class));

        ConversacionResponse response = conversacionService.crear(request);

        assertThat(response).isNotNull();
        verify(conversacionRepository).save(any(Conversacion.class));
    }

    @Test
    void crear_cuandoYaExiste_retornaExistente() {
        autenticarComo(autenticado);
        CrearConversacionRequest request = new CrearConversacionRequest(null, otroId, "Asunto");

        Conversacion existente = new Conversacion();
        existente.setId(conversacionId);

        when(usuarioRepository.findById(autenticadoId)).thenReturn(Optional.of(autenticado));
        when(usuarioRepository.findById(otroId)).thenReturn(Optional.of(otro));
        when(conversacionRepository.findByCursoIsNullAndParticipante1IdAndParticipante2Id(autenticadoId, otroId))
                .thenReturn(Optional.of(existente));
        when(mensajeRepository.countByConversacionIdAndRemitenteIdNotAndLeidoFalse(conversacionId, autenticadoId))
                .thenReturn(0L);
        when(conversacionMapper.toResponse(existente, 0L)).thenReturn(mock(ConversacionResponse.class));

        ConversacionResponse response = conversacionService.crear(request);

        assertThat(response).isNotNull();
        verify(conversacionRepository, never()).save(any());
    }

    // -------- obtener --------

    @Test
    void obtener_cuandoEsParticipante_devuelve() {
        autenticarComo(autenticado);
        Conversacion c = new Conversacion();
        c.setId(conversacionId);
        c.setParticipante1(autenticado);
        c.setParticipante2(otro);

        when(conversacionRepository.findWithParticipantesById(conversacionId)).thenReturn(Optional.of(c));
        when(mensajeRepository.countByConversacionIdAndRemitenteIdNotAndLeidoFalse(conversacionId, autenticadoId))
                .thenReturn(0L);
        when(conversacionMapper.toResponse(c, 0L)).thenReturn(mock(ConversacionResponse.class));

        ConversacionResponse response = conversacionService.obtener(conversacionId);

        assertThat(response).isNotNull();
    }

    @Test
    void obtener_cuandoNoEsParticipante_lanzaAccessDenied() {
        Usuario ajeno = new Usuario();
        ajeno.setId(UUID.randomUUID());
        ajeno.setRoles(Set.of(rolEstudiante()));
        autenticarComo(ajeno);

        Conversacion c = new Conversacion();
        c.setId(conversacionId);
        c.setParticipante1(autenticado);
        c.setParticipante2(otro);

        when(conversacionRepository.findWithParticipantesById(conversacionId)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> conversacionService.obtener(conversacionId))
                .isInstanceOf(AccessDeniedException.class);
    }

    private Rol rolEstudiante() {
        Rol r = new Rol();
        r.setNombre("ESTUDIANTE");
        return r;
    }
}