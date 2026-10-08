package com.kellyacademy.course.service;

import com.kellyacademy.course.dto.request.CrearUnidadRequest;
import com.kellyacademy.course.dto.request.ReordenarRequest;
import com.kellyacademy.course.dto.response.UnidadResumenResponse;
import com.kellyacademy.course.entity.Curso;
import com.kellyacademy.course.entity.Unidad;
import com.kellyacademy.course.mapper.UnidadMapper;
import com.kellyacademy.course.repository.CursoRepository;
import com.kellyacademy.course.repository.SemanaRepository;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnidadServiceTest {

    @Mock
    private UnidadRepository unidadRepository;

    @Mock
    private CursoRepository cursoRepository;

    @Mock
    private SemanaRepository semanaRepository;

    @Mock
    private UnidadMapper unidadMapper;

    @InjectMocks
    private UnidadService unidadService;

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

    private Curso cursoDeDocente() {
        Rol rolDocente = new Rol();
        rolDocente.setId(UUID.randomUUID());
        rolDocente.setNombre("DOCENTE");

        Usuario docente = new Usuario();
        docente.setId(docenteId);
        docente.setNombre("Docente");
        docente.setApellido("Test");
        docente.setCorreoElectronico("docente@kelly.com");
        docente.setEstado(EstadoUsuario.ACTIVO);
        docente.setRoles(Set.of(rolDocente));

        Curso curso = new Curso();
        curso.setId(UUID.randomUUID());
        curso.setDocente(docente);
        curso.setTitulo("Curso");
        return curso;
    }

    @Test
    void crear_numeroDuplicado_lanzaBusinessException() {

        Curso curso = cursoDeDocente();
        UUID cursoId = curso.getId();
        CrearUnidadRequest request = new CrearUnidadRequest(cursoId, 1, "Unidad 1", "Desc");

        when(cursoRepository.findWithDocenteById(cursoId)).thenReturn(Optional.of(curso));
        when(unidadRepository.existsByCursoIdAndNumero(cursoId, 1)).thenReturn(true);

        assertThatThrownBy(() -> unidadService.crear(request))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        org.assertj.core.api.Assertions.assertThat(ex.getCodigo())
                                .isEqualTo("NUMERO_DUPLICADO"));

        verify(unidadRepository, never()).save(any());
    }

    @Test
    void crear_cursoInexistente_lanzaResourceNotFoundException() {

        UUID cursoId = UUID.randomUUID();
        CrearUnidadRequest request = new CrearUnidadRequest(cursoId, 1, "Unidad 1", "Desc");

        when(cursoRepository.findWithDocenteById(cursoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> unidadService.crear(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(cursoId.toString());

        verify(unidadRepository, never()).save(any());
    }

    @Test
    void crear_docenteNoDuenoSinSerAdmin_lanzaAccessDeniedException() {

        UUID otroDocenteId = UUID.randomUUID();
        autenticarComo(otroDocenteId, "DOCENTE");

        Curso curso = cursoDeDocente();
        UUID cursoId = curso.getId();
        CrearUnidadRequest request = new CrearUnidadRequest(cursoId, 1, "Unidad 1", "Desc");

        when(cursoRepository.findWithDocenteById(cursoId)).thenReturn(Optional.of(curso));

        assertThatThrownBy(() -> unidadService.crear(request))
                .isInstanceOf(AccessDeniedException.class);

        verify(unidadRepository, never()).save(any());
    }

    @Test
    void eliminar_unidadConSemanas_lanzaBusinessException() {

        Curso curso = cursoDeDocente();
        UUID unidadId = UUID.randomUUID();
        Unidad unidad = new Unidad();
        unidad.setId(unidadId);
        unidad.setCurso(curso);

        when(unidadRepository.findWithCursoDocenteById(unidadId)).thenReturn(Optional.of(unidad));
        when(semanaRepository.existsByUnidadId(unidadId)).thenReturn(true);

        assertThatThrownBy(() -> unidadService.eliminar(unidadId))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        org.assertj.core.api.Assertions.assertThat(ex.getCodigo())
                                .isEqualTo("UNIDAD_CON_SEMANAS"));

        verify(unidadRepository, never()).delete(any());
    }

        @Test
        void reordenar_ok_devuelveNuevoOrden() {
        Curso curso = cursoDeDocente();
        Unidad u1 = unidad(curso, 1);
        Unidad u2 = unidad(curso, 2);
        List<Unidad> actuales = List.of(u1, u2);
        List<Unidad> finales = List.of(u2, u1);

        when(cursoRepository.findWithDocenteById(curso.getId())).thenReturn(Optional.of(curso));
        when(unidadRepository.findByCursoIdOrderByNumeroAsc(curso.getId()))
            .thenReturn(actuales, finales);
        when(unidadMapper.toResumenResponse(u2)).thenReturn(new UnidadResumenResponse(u2.getId(), 1, "U2"));
        when(unidadMapper.toResumenResponse(u1)).thenReturn(new UnidadResumenResponse(u1.getId(), 2, "U1"));

        List<UnidadResumenResponse> resultado = unidadService.reordenar(curso.getId(),
            new ReordenarRequest(List.of(
                new ReordenarRequest.ItemOrden(u2.getId(), 1),
                new ReordenarRequest.ItemOrden(u1.getId(), 2)
            )));

        assertThat(resultado).extracting(UnidadResumenResponse::id).containsExactly(u2.getId(), u1.getId());
        verify(unidadRepository, times(2)).saveAll(actuales);
        verify(unidadRepository, times(2)).flush();
        }

        @Test
        void reordenar_cuandoOrdenVacio_lanzaBusinessException() {
        Curso curso = cursoDeDocente();
        when(cursoRepository.findWithDocenteById(curso.getId())).thenReturn(Optional.of(curso));

        assertThatThrownBy(() -> unidadService.reordenar(curso.getId(), new ReordenarRequest(List.of())))
            .isInstanceOfSatisfying(BusinessException.class, ex ->
                org.assertj.core.api.Assertions.assertThat(ex.getCodigo()).isEqualTo("ORDEN_VACIO"));
        }

        @Test
        void reordenar_cuandoFaltanIds_lanzaBusinessException() {
        Curso curso = cursoDeDocente();
        Unidad u1 = unidad(curso, 1);
        Unidad u2 = unidad(curso, 2);
        when(cursoRepository.findWithDocenteById(curso.getId())).thenReturn(Optional.of(curso));
        when(unidadRepository.findByCursoIdOrderByNumeroAsc(curso.getId())).thenReturn(List.of(u1, u2));

        assertThatThrownBy(() -> unidadService.reordenar(curso.getId(), new ReordenarRequest(List.of(
            new ReordenarRequest.ItemOrden(u1.getId(), 1)
        )))).isInstanceOfSatisfying(BusinessException.class, ex ->
            org.assertj.core.api.Assertions.assertThat(ex.getCodigo()).isEqualTo("ORDEN_INCOMPLETO"));
        }

        @Test
        void reordenar_cuandoHayIdsAjenos_lanzaBusinessException() {
        Curso curso = cursoDeDocente();
        Unidad u1 = unidad(curso, 1);
        Unidad u2 = unidad(curso, 2);
        when(cursoRepository.findWithDocenteById(curso.getId())).thenReturn(Optional.of(curso));
        when(unidadRepository.findByCursoIdOrderByNumeroAsc(curso.getId())).thenReturn(List.of(u1, u2));

        assertThatThrownBy(() -> unidadService.reordenar(curso.getId(), new ReordenarRequest(List.of(
            new ReordenarRequest.ItemOrden(u1.getId(), 1),
            new ReordenarRequest.ItemOrden(u2.getId(), 2),
            new ReordenarRequest.ItemOrden(UUID.randomUUID(), 3)
        )))).isInstanceOfSatisfying(BusinessException.class, ex ->
            org.assertj.core.api.Assertions.assertThat(ex.getCodigo()).isEqualTo("ORDEN_CON_IDS_AJENOS"));
        }

        @Test
        void reordenar_cuandoNumerosDuplicados_lanzaBusinessException() {
        Curso curso = cursoDeDocente();
        Unidad u1 = unidad(curso, 1);
        Unidad u2 = unidad(curso, 2);
        when(cursoRepository.findWithDocenteById(curso.getId())).thenReturn(Optional.of(curso));
        when(unidadRepository.findByCursoIdOrderByNumeroAsc(curso.getId())).thenReturn(List.of(u1, u2));

        assertThatThrownBy(() -> unidadService.reordenar(curso.getId(), new ReordenarRequest(List.of(
            new ReordenarRequest.ItemOrden(u1.getId(), 1),
            new ReordenarRequest.ItemOrden(u2.getId(), 1)
        )))).isInstanceOfSatisfying(BusinessException.class, ex ->
            org.assertj.core.api.Assertions.assertThat(ex.getCodigo()).isEqualTo("ORDEN_CON_NUMEROS_DUPLICADOS"));
        }

        @Test
        void reordenar_cuandoNumerosNoContiguos_lanzaBusinessException() {
        Curso curso = cursoDeDocente();
        Unidad u1 = unidad(curso, 1);
        Unidad u2 = unidad(curso, 2);
        when(cursoRepository.findWithDocenteById(curso.getId())).thenReturn(Optional.of(curso));
        when(unidadRepository.findByCursoIdOrderByNumeroAsc(curso.getId())).thenReturn(List.of(u1, u2));

        assertThatThrownBy(() -> unidadService.reordenar(curso.getId(), new ReordenarRequest(List.of(
            new ReordenarRequest.ItemOrden(u1.getId(), 1),
            new ReordenarRequest.ItemOrden(u2.getId(), 3)
        )))).isInstanceOfSatisfying(BusinessException.class, ex ->
            org.assertj.core.api.Assertions.assertThat(ex.getCodigo()).isEqualTo("ORDEN_CON_NUMEROS_NO_CONTIGUOS"));
        }

        @Test
        void reordenar_cuandoMismoOrden_lanzaBusinessException() {
        Curso curso = cursoDeDocente();
        Unidad u1 = unidad(curso, 1);
        Unidad u2 = unidad(curso, 2);
        when(cursoRepository.findWithDocenteById(curso.getId())).thenReturn(Optional.of(curso));
        when(unidadRepository.findByCursoIdOrderByNumeroAsc(curso.getId())).thenReturn(List.of(u1, u2));

        assertThatThrownBy(() -> unidadService.reordenar(curso.getId(), new ReordenarRequest(List.of(
            new ReordenarRequest.ItemOrden(u1.getId(), 1),
            new ReordenarRequest.ItemOrden(u2.getId(), 2)
        )))).isInstanceOfSatisfying(BusinessException.class, ex ->
            org.assertj.core.api.Assertions.assertThat(ex.getCodigo()).isEqualTo("ORDEN_SIN_CAMBIOS"));
        }

        @Test
        void reordenar_cuandoNoEsDocenteDueno_lanzaAccessDenied() {
        Curso curso = cursoDeDocente();
        autenticarComo(UUID.randomUUID(), "DOCENTE");
        when(cursoRepository.findWithDocenteById(curso.getId())).thenReturn(Optional.of(curso));

        assertThatThrownBy(() -> unidadService.reordenar(curso.getId(), new ReordenarRequest(List.of(
            new ReordenarRequest.ItemOrden(UUID.randomUUID(), 1)
        )))).isInstanceOf(AccessDeniedException.class);
        }

        private Unidad unidad(Curso curso, int numero) {
        Unidad unidad = new Unidad();
        unidad.setId(UUID.randomUUID());
        unidad.setCurso(curso);
        unidad.setNumero(numero);
        unidad.setTitulo("U" + numero);
        return unidad;
        }
}