package com.kellyacademy.library.service;

import com.kellyacademy.course.enums.NivelCefr;
import com.kellyacademy.course.enums.TipoMaterial;
import com.kellyacademy.library.dto.request.ActualizarRecursoRequest;
import com.kellyacademy.library.dto.request.CrearRecursoRequest;
import com.kellyacademy.library.dto.response.DescargaRecursoResponse;
import com.kellyacademy.library.dto.response.RecursoResponse;
import com.kellyacademy.library.entity.RecursoBiblioteca;
import com.kellyacademy.library.entity.RecursoDescarga;
import com.kellyacademy.library.mapper.RecursoBibliotecaMapper;
import com.kellyacademy.library.repository.RecursoBibliotecaRepository;
import com.kellyacademy.library.repository.RecursoDescargaRepository;
import com.kellyacademy.security.user.CustomUserDetails;
import com.kellyacademy.shared.exception.BusinessException;
import com.kellyacademy.shared.exception.ResourceNotFoundException;
import com.kellyacademy.user.entity.Usuario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecursoBibliotecaServiceTest {

    @Mock private RecursoBibliotecaRepository recursoRepository;
    @Mock private RecursoDescargaRepository recursoDescargaRepository;
    @Mock private RecursoBibliotecaMapper recursoMapper;

    @InjectMocks private RecursoBibliotecaService recursoService;

    private UUID recursoId;
    private RecursoBiblioteca recurso;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        recursoId = UUID.randomUUID();
        recurso = new RecursoBiblioteca();
        recurso.setId(recursoId);
        recurso.setTitulo("Listening practice");
        recurso.setTipo(TipoMaterial.AUDIO);
        recurso.setNivelCefr(NivelCefr.A2);
        recurso.setUrlArchivo("https://cdn.kelly.com/audio.mp3");
        recurso.setContadorDescargas(0);

        usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setCorreoElectronico("est@kelly.com");
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

    private CrearRecursoRequest requestValido(String urlArchivo, String urlExterno) {
        return new CrearRecursoRequest(
                "Listening practice",
                "descripcion",
                "Listening",
                NivelCefr.A2,
                TipoMaterial.AUDIO,
                urlArchivo,
                urlExterno,
                null
        );
    }

    // -------- crear --------

    @Test
    void crear_conUrlArchivo_ok() {
        CrearRecursoRequest req = requestValido("https://cdn.kelly.com/audio.mp3", null);

        when(recursoMapper.toEntity(req)).thenReturn(new RecursoBiblioteca());
        when(recursoRepository.save(any(RecursoBiblioteca.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(recursoMapper.toResponse(any(RecursoBiblioteca.class)))
                .thenReturn(mock(RecursoResponse.class));

        recursoService.crear(req);

        verify(recursoRepository).save(any(RecursoBiblioteca.class));
    }

    @Test
    void crear_conUrlExterno_ok() {
        CrearRecursoRequest req = requestValido(null, "https://youtube.com/watch?v=xyz");

        when(recursoMapper.toEntity(req)).thenReturn(new RecursoBiblioteca());
        when(recursoRepository.save(any(RecursoBiblioteca.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(recursoMapper.toResponse(any(RecursoBiblioteca.class)))
                .thenReturn(mock(RecursoResponse.class));

        recursoService.crear(req);

        verify(recursoRepository).save(any(RecursoBiblioteca.class));
    }

    @Test
    void crear_conAmbasUrls_ok() {
        CrearRecursoRequest req = requestValido(
                "https://cdn.kelly.com/audio.mp3",
                "https://youtube.com/watch?v=xyz"
        );

        when(recursoMapper.toEntity(req)).thenReturn(new RecursoBiblioteca());
        when(recursoRepository.save(any(RecursoBiblioteca.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(recursoMapper.toResponse(any(RecursoBiblioteca.class)))
                .thenReturn(mock(RecursoResponse.class));

        recursoService.crear(req);

        verify(recursoRepository).save(any(RecursoBiblioteca.class));
    }

    @Test
    void crear_sinNingunaUrl_lanzaBusinessException() {
        CrearRecursoRequest req = requestValido(null, null);

        assertThatThrownBy(() -> recursoService.crear(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("al menos una URL");

        verifyNoInteractions(recursoRepository);
    }

    @Test
    void crear_conUrlsEnBlanco_lanzaBusinessException() {
        CrearRecursoRequest req = requestValido("   ", "   ");

        assertThatThrownBy(() -> recursoService.crear(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("al menos una URL");
    }

    @Test
    void crear_urlArchivoInvalida_lanzaBusinessException() {
        CrearRecursoRequest req = requestValido("no-es-una-url", null);

        assertThatThrownBy(() -> recursoService.crear(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("formato valido");
    }

    @Test
    void crear_urlExternoInvalida_lanzaBusinessException() {
        CrearRecursoRequest req = requestValido(null, "texto-suelto");

        assertThatThrownBy(() -> recursoService.crear(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("formato valido");
    }

    @Test
    void crear_urlConSchemeNoPermitido_lanzaBusinessException() {
        CrearRecursoRequest req = requestValido("ftp://archivo.com/x.mp3", null);

        assertThatThrownBy(() -> recursoService.crear(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("formato valido");
    }

    // -------- obtener --------

    @Test
    void obtener_existente_ok() {
        when(recursoRepository.findById(recursoId)).thenReturn(Optional.of(recurso));
        when(recursoMapper.toResponse(recurso)).thenReturn(mock(RecursoResponse.class));

        recursoService.obtener(recursoId);

        verify(recursoMapper).toResponse(recurso);
    }

    @Test
    void obtener_inexistente_lanzaResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(recursoRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recursoService.obtener(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // -------- actualizar --------

    @Test
    void actualizar_ok() {
        ActualizarRecursoRequest req = new ActualizarRecursoRequest(
                "Nuevo titulo", null, "Grammar", NivelCefr.B1,
                TipoMaterial.PDF, "https://cdn.kelly.com/new.pdf", null, null
        );

        when(recursoRepository.findById(recursoId)).thenReturn(Optional.of(recurso));
        when(recursoRepository.save(recurso)).thenReturn(recurso);
        when(recursoMapper.toResponse(recurso)).thenReturn(mock(RecursoResponse.class));

        recursoService.actualizar(recursoId, req);

        verify(recursoMapper).actualizarDesdeRequest(req, recurso);
    }

    @Test
    void actualizar_sinUrl_lanzaBusinessException() {
        ActualizarRecursoRequest req = new ActualizarRecursoRequest(
                "Nuevo titulo", null, null, null, TipoMaterial.PDF,
                null, null, null
        );

        when(recursoRepository.findById(recursoId)).thenReturn(Optional.of(recurso));

        assertThatThrownBy(() -> recursoService.actualizar(recursoId, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("al menos una URL");
    }

    @Test
    void actualizar_urlInvalida_lanzaBusinessException() {
        ActualizarRecursoRequest req = new ActualizarRecursoRequest(
                "Nuevo titulo", null, null, null, TipoMaterial.PDF,
                "no-url", null, null
        );

        when(recursoRepository.findById(recursoId)).thenReturn(Optional.of(recurso));

        assertThatThrownBy(() -> recursoService.actualizar(recursoId, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("formato valido");
    }

    @Test
    void actualizar_inexistente_lanzaResourceNotFound() {
        UUID id = UUID.randomUUID();
        ActualizarRecursoRequest req = new ActualizarRecursoRequest(
                "X", null, null, null, TipoMaterial.PDF,
                "https://cdn.kelly.com/x.pdf", null, null
        );

        when(recursoRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recursoService.actualizar(id, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // -------- eliminar --------

    @Test
    void eliminar_existente_ok() {
        when(recursoRepository.findById(recursoId)).thenReturn(Optional.of(recurso));

        recursoService.eliminar(recursoId);

        verify(recursoRepository).delete(recurso);
    }

    @Test
    void eliminar_inexistente_lanzaResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(recursoRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recursoService.eliminar(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // -------- descargar --------

    @Test
    void descargar_cuandoExiste_incrementaYRegistraTraza() {
        recurso.setContadorDescargas(5);
        autenticarComo(usuario);

        when(recursoRepository.incrementarContadorDescargas(recursoId)).thenReturn(1);
        when(recursoRepository.findById(recursoId)).thenReturn(Optional.of(recurso));

        DescargaRecursoResponse resp = recursoService.descargar(recursoId, "203.0.113.10");

        assertThat(resp.id()).isEqualTo(recursoId);
        assertThat(resp.urlArchivo()).isEqualTo("https://cdn.kelly.com/audio.mp3");
        assertThat(resp.urlExterno()).isNull();
        assertThat(resp.contadorDescargas()).isEqualTo(5);

        verify(recursoRepository).incrementarContadorDescargas(recursoId);

        ArgumentCaptor<RecursoDescarga> captor = ArgumentCaptor.forClass(RecursoDescarga.class);
        verify(recursoDescargaRepository).save(captor.capture());

        RecursoDescarga traza = captor.getValue();
        assertThat(traza.getRecurso()).isEqualTo(recurso);
        assertThat(traza.getUsuario()).isEqualTo(usuario);
        assertThat(traza.getIp()).isEqualTo("203.0.113.10");
        assertThat(traza.getFechaDescarga()).isNotNull();
    }

    @Test
    void descargar_cuandoNoExiste_lanzaResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(recursoRepository.incrementarContadorDescargas(id)).thenReturn(0);

        assertThatThrownBy(() -> recursoService.descargar(id, "203.0.113.10"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(recursoRepository, never()).findById(any());
        verifyNoInteractions(recursoDescargaRepository);
    }

    @Test
    void descargar_recursoSinUrl_lanzaBusinessException() {
        recurso.setUrlArchivo(null);
        recurso.setUrlExterno(null);
        autenticarComo(usuario);

        when(recursoRepository.incrementarContadorDescargas(recursoId)).thenReturn(1);
        when(recursoRepository.findById(recursoId)).thenReturn(Optional.of(recurso));

        assertThatThrownBy(() -> recursoService.descargar(recursoId, "203.0.113.10"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("al menos una URL");

        verifyNoInteractions(recursoDescargaRepository);
    }
}