package com.kellyacademy.course.service;

import com.kellyacademy.course.dto.request.CrearTareaRequest;
import com.kellyacademy.course.entity.Curso;
import com.kellyacademy.course.entity.Semana;
import com.kellyacademy.course.entity.Tarea;
import com.kellyacademy.course.entity.Unidad;
import com.kellyacademy.course.mapper.TareaMapper;
import com.kellyacademy.course.repository.SemanaRepository;
import com.kellyacademy.course.repository.TareaRepository;
import com.kellyacademy.security.user.CustomUserDetails;
import com.kellyacademy.shared.exception.BusinessException;
import com.kellyacademy.shared.exception.ResourceNotFoundException;
import com.kellyacademy.user.entity.Rol;
import com.kellyacademy.user.entity.Usuario;
import com.kellyacademy.user.enums.EstadoUsuario;
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

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TareaServiceTest {

    @Mock private TareaRepository tareaRepository;
    @Mock private SemanaRepository semanaRepository;
    @Mock private TareaMapper tareaMapper;

    @InjectMocks
    private TareaService tareaService;

    private UUID adminId;
    private UUID docenteId;

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
        docenteId = UUID.randomUUID();
        autenticarComo(adminId, "ADMINISTRADOR");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(UUID id, String nombreRol) {
        Rol rol = new Rol();
        rol.setId(UUID.randomUUID());
        rol.setNombre(nombreRol);

        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setCorreoElectronico("auth@kelly.com");
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario.setRoles(Set.of(rol));

        CustomUserDetails details = new CustomUserDetails(usuario);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private Semana semanaDeDocente() {
        Rol rolDocente = new Rol();
        rolDocente.setId(UUID.randomUUID());
        rolDocente.setNombre("DOCENTE");

        Usuario docente = new Usuario();
        docente.setId(docenteId);
        docente.setEstado(EstadoUsuario.ACTIVO);
        docente.setRoles(Set.of(rolDocente));

        Curso curso = new Curso();
        curso.setId(UUID.randomUUID());
        curso.setDocente(docente);

        Unidad unidad = new Unidad();
        unidad.setId(UUID.randomUUID());
        unidad.setCurso(curso);

        Semana semana = new Semana();
        semana.setId(UUID.randomUUID());
        semana.setUnidad(unidad);
        return semana;
    }

    @Test
    void crear_instruccionesUrlInvalida_lanzaUrlInvalida() {

        Semana semana = semanaDeDocente();
        CrearTareaRequest request = new CrearTareaRequest(
                semana.getId(), "Tarea 1", "Desc",
                "no-es-url", LocalDateTime.now().plusDays(7), 100
        );

        when(semanaRepository.findWithUnidadCursoDocenteById(semana.getId()))
                .thenReturn(Optional.of(semana));

        assertThatThrownBy(() -> tareaService.crear(request))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getCodigo()).isEqualTo("URL_INVALIDA"));

        verify(tareaRepository, never()).save(any());
    }

    @Test
    void crear_instruccionesUrlNull_permitido() {

        Semana semana = semanaDeDocente();
        CrearTareaRequest request = new CrearTareaRequest(
                semana.getId(), "Tarea 1", "Desc",
                null, LocalDateTime.now().plusDays(7), 100
        );

        Tarea tareaMapeada = new Tarea();
        tareaMapeada.setTitulo(request.titulo());

        Tarea guardada = new Tarea();
        UUID idGenerado = UUID.randomUUID();
        guardada.setId(idGenerado);
        guardada.setSemana(semana);

        when(semanaRepository.findWithUnidadCursoDocenteById(semana.getId()))
                .thenReturn(Optional.of(semana));
        when(tareaMapper.toEntity(request)).thenReturn(tareaMapeada);
        when(tareaRepository.save(any(Tarea.class))).thenReturn(guardada);
        when(tareaRepository.findWithSemanaCursoDocenteById(idGenerado))
                .thenReturn(Optional.of(guardada));
        when(tareaMapper.toResponse(guardada)).thenReturn(null);

        tareaService.crear(request);

        verify(tareaRepository).save(any(Tarea.class));
    }

    @Test
    void crear_docenteAjenoSinSerAdmin_lanzaAccessDeniedException() {

        UUID otroDocenteId = UUID.randomUUID();
        autenticarComo(otroDocenteId, "DOCENTE");

        Semana semana = semanaDeDocente();
        CrearTareaRequest request = new CrearTareaRequest(
                semana.getId(), "Tarea 1", "Desc",
                null, null, 100
        );

        when(semanaRepository.findWithUnidadCursoDocenteById(semana.getId()))
                .thenReturn(Optional.of(semana));

        assertThatThrownBy(() -> tareaService.crear(request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void crear_semanaInexistente_lanzaResourceNotFoundException() {

        UUID semanaId = UUID.randomUUID();
        CrearTareaRequest request = new CrearTareaRequest(
                semanaId, "Tarea 1", "Desc",
                null, null, 100
        );

        when(semanaRepository.findWithUnidadCursoDocenteById(semanaId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> tareaService.crear(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}