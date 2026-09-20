package com.kellyacademy.course.service;

import com.kellyacademy.course.dto.request.CrearClaseRequest;
import com.kellyacademy.course.entity.Clase;
import com.kellyacademy.course.entity.Curso;
import com.kellyacademy.course.entity.Semana;
import com.kellyacademy.course.entity.Unidad;
import com.kellyacademy.course.mapper.ClaseMapper;
import com.kellyacademy.course.repository.ClaseRepository;
import com.kellyacademy.course.repository.SemanaRepository;
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
class ClaseServiceTest {

    @Mock private ClaseRepository claseRepository;
    @Mock private SemanaRepository semanaRepository;
    @Mock private ClaseMapper claseMapper;

    @InjectMocks
    private ClaseService claseService;

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
        docente.setNombre("Docente");
        docente.setApellido("Test");
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
    void crear_urlInvalida_lanzaBusinessException() {

        Semana semana = semanaDeDocente();
        CrearClaseRequest request = new CrearClaseRequest(
                semana.getId(), "Clase 1", "Desc",
                "no-es-una-url", null, null, null, null
        );

        when(semanaRepository.findWithUnidadCursoDocenteById(semana.getId()))
                .thenReturn(Optional.of(semana));

        assertThatThrownBy(() -> claseService.crear(request))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getCodigo()).isEqualTo("URL_INVALIDA"));

        verify(claseRepository, never()).save(any());
    }

    @Test
    void crear_semanaInexistente_lanzaResourceNotFoundException() {

        UUID semanaId = UUID.randomUUID();
        CrearClaseRequest request = new CrearClaseRequest(
                semanaId, "Clase 1", "Desc",
                "https://meet.example.com/a", null, null, null, null
        );

        when(semanaRepository.findWithUnidadCursoDocenteById(semanaId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> claseService.crear(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(semanaId.toString());
    }

    @Test
    void crear_docenteAjenoSinSerAdmin_lanzaAccessDeniedException() {

        UUID otroDocenteId = UUID.randomUUID();
        autenticarComo(otroDocenteId, "DOCENTE");

        Semana semana = semanaDeDocente();
        CrearClaseRequest request = new CrearClaseRequest(
                semana.getId(), "Clase 1", "Desc",
                "https://meet.example.com/a", null, null, null, null
        );

        when(semanaRepository.findWithUnidadCursoDocenteById(semana.getId()))
                .thenReturn(Optional.of(semana));

        assertThatThrownBy(() -> claseService.crear(request))
                .isInstanceOf(AccessDeniedException.class);

        verify(claseRepository, never()).save(any());
    }

    @Test
    void crear_adminPuedeCrearEnCualquierSemana() {

        Semana semana = semanaDeDocente();
        CrearClaseRequest request = new CrearClaseRequest(
                semana.getId(), "Clase 1", "Desc",
                "https://meet.example.com/a", "rtmp://grab.example.com/b",
                null, 60, "Sala 1"
        );

        Clase claseMapeada = new Clase();
        claseMapeada.setTitulo(request.titulo());

        Clase guardada = new Clase();
        UUID idGenerado = UUID.randomUUID();
        guardada.setId(idGenerado);
        guardada.setSemana(semana);
        guardada.setTitulo(request.titulo());

        when(semanaRepository.findWithUnidadCursoDocenteById(semana.getId()))
                .thenReturn(Optional.of(semana));
        when(claseMapper.toEntity(request)).thenReturn(claseMapeada);
        when(claseRepository.save(any(Clase.class))).thenReturn(guardada);
        when(claseRepository.findWithSemanaCursoDocenteById(idGenerado))
                .thenReturn(Optional.of(guardada));
        when(claseMapper.toResponse(guardada)).thenReturn(null);

        claseService.crear(request);

        verify(claseRepository).save(any(Clase.class));
    }
}