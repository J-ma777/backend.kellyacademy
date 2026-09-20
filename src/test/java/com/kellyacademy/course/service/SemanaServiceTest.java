package com.kellyacademy.course.service;

import com.kellyacademy.course.dto.request.CrearSemanaRequest;
import com.kellyacademy.course.dto.response.SemanaResponse;
import com.kellyacademy.course.entity.Curso;
import com.kellyacademy.course.entity.Semana;
import com.kellyacademy.course.entity.Unidad;
import com.kellyacademy.course.mapper.SemanaMapper;
import com.kellyacademy.course.repository.ClaseRepository;
import com.kellyacademy.course.repository.MaterialRepository;
import com.kellyacademy.course.repository.SemanaRepository;
import com.kellyacademy.course.repository.TareaRepository;
import com.kellyacademy.course.repository.UnidadRepository;
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
class SemanaServiceTest {

    @Mock private SemanaRepository semanaRepository;
    @Mock private UnidadRepository unidadRepository;
    @Mock private ClaseRepository claseRepository;
    @Mock private MaterialRepository materialRepository;
    @Mock private TareaRepository tareaRepository;
    @Mock private SemanaMapper semanaMapper;

    @InjectMocks
    private SemanaService semanaService;

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

    private Unidad unidadDeDocente() {
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
        return unidad;
    }

    @Test
    void crear_numeroDuplicado_lanzaBusinessException() {

        Unidad unidad = unidadDeDocente();
        UUID unidadId = unidad.getId();
        CrearSemanaRequest request = new CrearSemanaRequest(unidadId, 1, "Semana 1", "Desc");

        when(unidadRepository.findWithCursoDocenteById(unidadId)).thenReturn(Optional.of(unidad));
        when(semanaRepository.existsByUnidadIdAndNumero(unidadId, 1)).thenReturn(true);

        assertThatThrownBy(() -> semanaService.crear(request))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getCodigo()).isEqualTo("NUMERO_DUPLICADO"));

        verify(semanaRepository, never()).save(any());
    }

    @Test
    void crear_unidadInexistente_lanzaResourceNotFoundException() {

        UUID unidadId = UUID.randomUUID();
        CrearSemanaRequest request = new CrearSemanaRequest(unidadId, 1, "Semana 1", "Desc");

        when(unidadRepository.findWithCursoDocenteById(unidadId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> semanaService.crear(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(unidadId.toString());
    }

    @Test
    void eliminar_semanaConContenido_lanzaBusinessException() {

        Unidad unidad = unidadDeDocente();
        UUID semanaId = UUID.randomUUID();
        Semana semana = new Semana();
        semana.setId(semanaId);
        semana.setUnidad(unidad);

        when(semanaRepository.findWithUnidadCursoDocenteById(semanaId))
                .thenReturn(Optional.of(semana));
        when(claseRepository.existsBySemanaId(semanaId)).thenReturn(true);

        assertThatThrownBy(() -> semanaService.eliminar(semanaId))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getCodigo()).isEqualTo("SEMANA_CON_CONTENIDO"));

        verify(semanaRepository, never()).delete(any());
    }

    @Test
    void marcarActual_yaEsActual_esIdempotenteYNoTocaLaBd() {

        Unidad unidad = unidadDeDocente();
        UUID semanaId = UUID.randomUUID();
        Semana semana = new Semana();
        semana.setId(semanaId);
        semana.setUnidad(unidad);
        semana.setEsActual(true);

        SemanaResponse response = new SemanaResponse(
                semanaId, unidad.getId(), 1, "Semana 1", "Desc",
                true, null, null
        );

        when(semanaRepository.findWithUnidadCursoDocenteById(semanaId))
                .thenReturn(Optional.of(semana));
        when(semanaMapper.toResponse(semana)).thenReturn(response);

        SemanaResponse resultado = semanaService.marcarActual(semanaId);

        assertThat(resultado.esActual()).isTrue();

        // No debe buscar la anterior ni modificarla: ya era actual.
        verify(semanaRepository, never()).findByUnidadIdAndEsActualTrue(any());
    }

    @Test
    void marcarActual_desmarcaLaAnteriorYMarcaLaNueva() {

        Unidad unidad = unidadDeDocente();
        UUID unidadId = unidad.getId();
        UUID nuevaId = UUID.randomUUID();

        Semana anterior = new Semana();
        anterior.setId(UUID.randomUUID());
        anterior.setUnidad(unidad);
        anterior.setEsActual(true);

        Semana nueva = new Semana();
        nueva.setId(nuevaId);
        nueva.setUnidad(unidad);
        nueva.setEsActual(false);

        SemanaResponse response = new SemanaResponse(
                nuevaId, unidadId, 2, "Semana 2", "Desc",
                true, null, null
        );

        when(semanaRepository.findWithUnidadCursoDocenteById(nuevaId)).thenReturn(Optional.of(nueva));
        when(semanaRepository.findByUnidadIdAndEsActualTrue(unidadId))
                .thenReturn(Optional.of(anterior));
        when(semanaMapper.toResponse(nueva)).thenReturn(response);

        SemanaResponse resultado = semanaService.marcarActual(nuevaId);

        assertThat(anterior.getEsActual()).isFalse();
        assertThat(nueva.getEsActual()).isTrue();
        assertThat(resultado.esActual()).isTrue();
    }
}