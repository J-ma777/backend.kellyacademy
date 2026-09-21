package com.kellyacademy.library.service;

import com.kellyacademy.course.enums.NivelCefr;
import com.kellyacademy.course.enums.TipoMaterial;
import com.kellyacademy.library.dto.request.ActualizarRecursoRequest;
import com.kellyacademy.library.dto.request.CrearRecursoRequest;
import com.kellyacademy.library.dto.response.RecursoResponse;
import com.kellyacademy.library.entity.RecursoBiblioteca;
import com.kellyacademy.library.mapper.RecursoBibliotecaMapper;
import com.kellyacademy.library.repository.RecursoBibliotecaRepository;
import com.kellyacademy.shared.exception.BusinessException;
import com.kellyacademy.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecursoBibliotecaServiceTest {

    @Mock private RecursoBibliotecaRepository recursoRepository;
    @Mock private RecursoBibliotecaMapper recursoMapper;

    @InjectMocks private RecursoBibliotecaService recursoService;

    private UUID recursoId;
    private RecursoBiblioteca recurso;

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
        // ftp no esta en el patron (https?|rtmp|rtsp).
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
}