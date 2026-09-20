package com.kellyacademy.course.service;

import com.kellyacademy.course.dto.request.CrearMaterialRequest;
import com.kellyacademy.course.entity.Curso;
import com.kellyacademy.course.entity.Material;
import com.kellyacademy.course.entity.Semana;
import com.kellyacademy.course.entity.Unidad;
import com.kellyacademy.course.enums.TipoMaterial;
import com.kellyacademy.course.mapper.MaterialMapper;
import com.kellyacademy.course.repository.MaterialRepository;
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
class MaterialServiceTest {

    @Mock private MaterialRepository materialRepository;
    @Mock private SemanaRepository semanaRepository;
    @Mock private MaterialMapper materialMapper;

    @InjectMocks
    private MaterialService materialService;

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
    void crear_sinUrlArchivoNiUrlExterno_lanzaMaterialSinUrl() {

        Semana semana = semanaDeDocente();
        CrearMaterialRequest request = new CrearMaterialRequest(
                semana.getId(), "Material 1", "Desc",
                null, TipoMaterial.PDF, null, null
        );

        when(semanaRepository.findWithUnidadCursoDocenteById(semana.getId()))
                .thenReturn(Optional.of(semana));

        assertThatThrownBy(() -> materialService.crear(request))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getCodigo()).isEqualTo("MATERIAL_SIN_URL"));

        verify(materialRepository, never()).save(any());
    }

    @Test
    void crear_urlExternaInvalida_lanzaUrlInvalida() {

        Semana semana = semanaDeDocente();
        CrearMaterialRequest request = new CrearMaterialRequest(
                semana.getId(), "Material 1", "Desc",
                null, TipoMaterial.ENLACE, null, "no-es-url"
        );

        when(semanaRepository.findWithUnidadCursoDocenteById(semana.getId()))
                .thenReturn(Optional.of(semana));

        assertThatThrownBy(() -> materialService.crear(request))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getCodigo()).isEqualTo("URL_INVALIDA"));
    }

    @Test
    void crear_conUrlArchivoValida_exitoso() {

        Semana semana = semanaDeDocente();
        CrearMaterialRequest request = new CrearMaterialRequest(
                semana.getId(), "Material 1", "Desc",
                "https://cdn.example.com/file.pdf", TipoMaterial.PDF, null, null
        );

        Material materialMapeado = new Material();
        materialMapeado.setTitulo(request.titulo());

        Material guardado = new Material();
        UUID idGenerado = UUID.randomUUID();
        guardado.setId(idGenerado);
        guardado.setSemana(semana);
        guardado.setTitulo(request.titulo());

        when(semanaRepository.findWithUnidadCursoDocenteById(semana.getId()))
                .thenReturn(Optional.of(semana));
        when(materialMapper.toEntity(request)).thenReturn(materialMapeado);
        when(materialRepository.save(any(Material.class))).thenReturn(guardado);
        when(materialRepository.findWithSemanaCursoDocenteById(idGenerado))
                .thenReturn(Optional.of(guardado));
        when(materialMapper.toResponse(guardado)).thenReturn(null);

        materialService.crear(request);

        verify(materialRepository).save(any(Material.class));
    }

    @Test
    void crear_docenteAjenoSinSerAdmin_lanzaAccessDeniedException() {

        UUID otroDocenteId = UUID.randomUUID();
        autenticarComo(otroDocenteId, "DOCENTE");

        Semana semana = semanaDeDocente();
        CrearMaterialRequest request = new CrearMaterialRequest(
                semana.getId(), "Material 1", "Desc",
                "https://cdn.example.com/file.pdf", TipoMaterial.PDF, null, null
        );

        when(semanaRepository.findWithUnidadCursoDocenteById(semana.getId()))
                .thenReturn(Optional.of(semana));

        assertThatThrownBy(() -> materialService.crear(request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void crear_semanaInexistente_lanzaResourceNotFoundException() {

        UUID semanaId = UUID.randomUUID();
        CrearMaterialRequest request = new CrearMaterialRequest(
                semanaId, "Material 1", "Desc",
                "https://cdn.example.com/file.pdf", TipoMaterial.PDF, null, null
        );

        when(semanaRepository.findWithUnidadCursoDocenteById(semanaId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> materialService.crear(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}