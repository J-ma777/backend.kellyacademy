package com.kellyacademy.calendar.service;

import com.kellyacademy.calendar.dto.request.ActualizarDisponibilidadRequest;
import com.kellyacademy.calendar.dto.request.CrearDisponibilidadRequest;
import com.kellyacademy.calendar.dto.response.DisponibilidadResponse;
import com.kellyacademy.calendar.entity.DisponibilidadTutoria;
import com.kellyacademy.calendar.mapper.DisponibilidadMapper;
import com.kellyacademy.calendar.repository.DisponibilidadTutoriaRepository;
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

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DisponibilidadTutoriaServiceTest {

    @Mock private DisponibilidadTutoriaRepository disponibilidadRepository;
    @Mock private DisponibilidadMapper disponibilidadMapper;
    @Mock private UsuarioRepository usuarioRepository;

    @InjectMocks private DisponibilidadTutoriaService disponibilidadService;

    private UUID docenteId;
    private Usuario docente;
    private DisponibilidadTutoria entity;

    @BeforeEach
    void setUp() {
        docenteId = UUID.randomUUID();

        Rol rolDocente = new Rol();
        rolDocente.setNombre("DOCENTE");

        docente = new Usuario();
        docente.setId(docenteId);
        docente.setNombre("Ana");
        docente.setApellido("Torres");
        docente.setRoles(Set.of(rolDocente));

        entity = new DisponibilidadTutoria();
        entity.setId(UUID.randomUUID());
        entity.setDocente(docente);
        entity.setDiaSemana(DayOfWeek.MONDAY);
        entity.setHoraInicio(LocalTime.of(9, 0));
        entity.setHoraFin(LocalTime.of(10, 0));
        entity.setBloqueada(false);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(Usuario usuario) {
        CustomUserDetails userDetails = new CustomUserDetails(usuario);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()
                )
        );
    }

    // -------- crear --------

    @Test
    void crear_cuandoTodoValido_retornaResponse() {
        autenticarComo(docente);

        CrearDisponibilidadRequest req = new CrearDisponibilidadRequest(
                docenteId, DayOfWeek.TUESDAY, LocalTime.of(10, 0), LocalTime.of(11, 0)
        );

        when(usuarioRepository.findById(docenteId)).thenReturn(Optional.of(docente));
        when(disponibilidadRepository.findSolapadas(
                docenteId, DayOfWeek.TUESDAY, LocalTime.of(10, 0), LocalTime.of(11, 0), null))
                .thenReturn(List.of());
        when(disponibilidadMapper.toEntity(req)).thenReturn(new DisponibilidadTutoria());
        when(disponibilidadRepository.save(any(DisponibilidadTutoria.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(disponibilidadMapper.toResponse(any(DisponibilidadTutoria.class)))
                .thenReturn(mock(DisponibilidadResponse.class));

        DisponibilidadResponse response = disponibilidadService.crear(req);

        assertThat(response).isNotNull();
        verify(disponibilidadRepository).save(any(DisponibilidadTutoria.class));
    }

    @Test
    void crear_horaFinAntesDeInicio_lanzaBusinessException() {
        autenticarComo(docente);

        CrearDisponibilidadRequest req = new CrearDisponibilidadRequest(
                docenteId, DayOfWeek.TUESDAY, LocalTime.of(11, 0), LocalTime.of(10, 0)
        );

        assertThatThrownBy(() -> disponibilidadService.crear(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("posterior");

        verifyNoInteractions(disponibilidadRepository, usuarioRepository);
    }

    @Test
    void crear_solapeConOtroBloque_lanzaBusinessException() {
        autenticarComo(docente);

        CrearDisponibilidadRequest req = new CrearDisponibilidadRequest(
                docenteId, DayOfWeek.MONDAY, LocalTime.of(9, 30), LocalTime.of(10, 30)
        );

        when(disponibilidadRepository.findSolapadas(
                docenteId, DayOfWeek.MONDAY, LocalTime.of(9, 30), LocalTime.of(10, 30), null))
                .thenReturn(List.of(entity));

        assertThatThrownBy(() -> disponibilidadService.crear(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("solapa");

        verify(usuarioRepository, never()).findById(any());
    }

    @Test
    void crear_docenteAjeno_lanzaAccessDenied() {
        Rol rolDocente = new Rol();
        rolDocente.setNombre("DOCENTE");
        Usuario otro = new Usuario();
        otro.setId(UUID.randomUUID());
        otro.setRoles(Set.of(rolDocente));
        autenticarComo(otro);

        CrearDisponibilidadRequest req = new CrearDisponibilidadRequest(
                docenteId, DayOfWeek.TUESDAY, LocalTime.of(10, 0), LocalTime.of(11, 0)
        );

        assertThatThrownBy(() -> disponibilidadService.crear(req))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void crear_comoAdmin_puedeCrearParaCualquierDocente() {
        Rol rolAdmin = new Rol();
        rolAdmin.setNombre("ADMINISTRADOR");
        Usuario admin = new Usuario();
        admin.setId(UUID.randomUUID());
        admin.setRoles(Set.of(rolAdmin));
        autenticarComo(admin);

        CrearDisponibilidadRequest req = new CrearDisponibilidadRequest(
                docenteId, DayOfWeek.TUESDAY, LocalTime.of(10, 0), LocalTime.of(11, 0)
        );

        when(usuarioRepository.findById(docenteId)).thenReturn(Optional.of(docente));
        when(disponibilidadRepository.findSolapadas(
                docenteId, DayOfWeek.TUESDAY, LocalTime.of(10, 0), LocalTime.of(11, 0), null))
                .thenReturn(List.of());
        when(disponibilidadMapper.toEntity(req)).thenReturn(new DisponibilidadTutoria());
        when(disponibilidadRepository.save(any(DisponibilidadTutoria.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(disponibilidadMapper.toResponse(any(DisponibilidadTutoria.class)))
                .thenReturn(mock(DisponibilidadResponse.class));

        DisponibilidadResponse response = disponibilidadService.crear(req);

        assertThat(response).isNotNull();
    }

    // -------- obtener --------

    @Test
    void obtener_inexistente_lanzaResourceNotFound() {
        autenticarComo(docente);
        UUID id = UUID.randomUUID();

        when(disponibilidadRepository.findWithDocenteById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> disponibilidadService.obtener(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void obtener_existente_retornaResponse() {
        autenticarComo(docente);

        when(disponibilidadRepository.findWithDocenteById(entity.getId()))
                .thenReturn(Optional.of(entity));
        when(disponibilidadMapper.toResponse(entity))
                .thenReturn(mock(DisponibilidadResponse.class));

        disponibilidadService.obtener(entity.getId());

        verify(disponibilidadMapper).toResponse(entity);
    }

    // -------- actualizar --------

    @Test
    void actualizar_propio_ok() {
        autenticarComo(docente);

        ActualizarDisponibilidadRequest req = new ActualizarDisponibilidadRequest(
                DayOfWeek.WEDNESDAY, LocalTime.of(14, 0), LocalTime.of(15, 0), true
        );

        when(disponibilidadRepository.findWithDocenteById(entity.getId()))
                .thenReturn(Optional.of(entity));
        when(disponibilidadRepository.findSolapadas(
                docenteId, DayOfWeek.WEDNESDAY, LocalTime.of(14, 0), LocalTime.of(15, 0), entity.getId()))
                .thenReturn(List.of());
        when(disponibilidadRepository.save(entity)).thenReturn(entity);
        when(disponibilidadMapper.toResponse(entity)).thenReturn(mock(DisponibilidadResponse.class));

        disponibilidadService.actualizar(entity.getId(), req);

        verify(disponibilidadMapper).actualizarDesdeRequest(req, entity);
        verify(disponibilidadRepository).save(entity);
    }

    @Test
    void actualizar_horaInvalida_lanzaBusinessException() {
        autenticarComo(docente);

        ActualizarDisponibilidadRequest req = new ActualizarDisponibilidadRequest(
                DayOfWeek.WEDNESDAY, LocalTime.of(15, 0), LocalTime.of(14, 0), false
        );

        when(disponibilidadRepository.findWithDocenteById(entity.getId()))
                .thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> disponibilidadService.actualizar(entity.getId(), req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("posterior");
    }

    @Test
    void actualizar_solapeConOtroBloque_lanzaBusinessException() {
        autenticarComo(docente);

        ActualizarDisponibilidadRequest req = new ActualizarDisponibilidadRequest(
                DayOfWeek.MONDAY, LocalTime.of(9, 30), LocalTime.of(10, 30), false
        );

        when(disponibilidadRepository.findWithDocenteById(entity.getId()))
                .thenReturn(Optional.of(entity));
        when(disponibilidadRepository.findSolapadas(
                docenteId, DayOfWeek.MONDAY, LocalTime.of(9, 30), LocalTime.of(10, 30), entity.getId()))
                .thenReturn(List.of(new DisponibilidadTutoria()));

        assertThatThrownBy(() -> disponibilidadService.actualizar(entity.getId(), req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("solapa");
    }

    @Test
    void actualizar_ajeno_lanzaAccessDenied() {
        Rol rolDocente = new Rol();
        rolDocente.setNombre("DOCENTE");
        Usuario otro = new Usuario();
        otro.setId(UUID.randomUUID());
        otro.setRoles(Set.of(rolDocente));
        autenticarComo(otro);

        ActualizarDisponibilidadRequest req = new ActualizarDisponibilidadRequest(
                DayOfWeek.WEDNESDAY, LocalTime.of(14, 0), LocalTime.of(15, 0), true
        );

        when(disponibilidadRepository.findWithDocenteById(entity.getId()))
                .thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> disponibilidadService.actualizar(entity.getId(), req))
                .isInstanceOf(AccessDeniedException.class);
    }

    // -------- eliminar --------

    @Test
    void eliminar_existente_ok() {
        autenticarComo(docente);

        when(disponibilidadRepository.findWithDocenteById(entity.getId()))
                .thenReturn(Optional.of(entity));

        disponibilidadService.eliminar(entity.getId());

        verify(disponibilidadRepository).delete(entity);
    }

    @Test
    void eliminar_inexistente_lanzaResourceNotFound() {
        autenticarComo(docente);
        UUID id = UUID.randomUUID();

        when(disponibilidadRepository.findWithDocenteById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> disponibilidadService.eliminar(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}