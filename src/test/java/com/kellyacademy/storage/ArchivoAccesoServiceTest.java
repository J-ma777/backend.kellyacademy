package com.kellyacademy.storage;

import com.kellyacademy.communication.repository.AnuncioRepository;
import com.kellyacademy.course.entity.Curso;
import com.kellyacademy.course.repository.MaterialRepository;
import com.kellyacademy.course.repository.TareaRepository;
import com.kellyacademy.course.service.AccesoCursoService;
import com.kellyacademy.support.TestData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArchivoAccesoServiceTest {
    @Mock ArchivoRepository archivos;
    @Mock AccesoCursoService acceso;
    @Mock MaterialRepository materiales;
    @Mock TareaRepository tareas;
    @Mock AnuncioRepository anuncios;
    @Mock CloudinaryGateway cloudinary;
    @Mock R2Gateway r2;
    @Mock OfficePreviewGateway office;
    @InjectMocks ArchivoAccesoService service;
    @AfterEach void limpiar() { SecurityContextHolder.clearContext(); }

    @Test void laImagenPropiaSeEntregaConEnlaceTemporalSinDivulgarUrlHistorica() {
        var alumno = TestData.usuario("ESTUDIANTE"); TestData.autenticar(alumno);
        var curso = new Curso(); curso.setId(UUID.randomUUID());
        var archivo = imagen(curso.getId(), alumno.getId(), "ENTREGA");
        when(acceso.obtener(curso.getId())).thenReturn(curso);
        when(archivos.findById(archivo.getId())).thenReturn(Optional.of(archivo));
        when(cloudinary.downloadUrl(archivo.getPublicId(), archivo.getMime()))
                .thenReturn("https://api.cloudinary.com/download?expires_at=temporal");

        var result = service.obtener(curso.getId(), archivo.getId());
        assertThat(result.expiraEnSegundos()).isEqualTo(300);
        assertThat(result.url()).doesNotContain("firma-antigua").contains("expires_at=");
    }

    @Test void otroAlumnoNoPuedeVerLaEntregaPrivada() {
        TestData.autenticar(TestData.usuario("ESTUDIANTE"));
        var curso = new Curso(); curso.setId(UUID.randomUUID());
        var archivo = imagen(curso.getId(), UUID.randomUUID(), "ENTREGA");
        when(acceso.obtener(curso.getId())).thenReturn(curso);
        when(archivos.findById(archivo.getId())).thenReturn(Optional.of(archivo));

        assertThatThrownBy(() -> service.obtener(curso.getId(), archivo.getId()))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(cloudinary);
    }

    @Test void imagenEnAnuncioPublicadoSeEntregaASusAlumnos() {
        TestData.autenticar(TestData.usuario("ESTUDIANTE"));
        var curso = new Curso(); curso.setId(UUID.randomUUID());
        var archivo = imagen(curso.getId(), UUID.randomUUID(), "ANUNCIO");
        when(acceso.obtener(curso.getId())).thenReturn(curso);
        when(archivos.findById(archivo.getId())).thenReturn(Optional.of(archivo));
        when(anuncios.existsByImagenUrlAndCursoIdAndActivoTrueAndPublicadoTrue(archivo.getUrl(), curso.getId())).thenReturn(true);
        when(cloudinary.downloadUrl(archivo.getPublicId(), archivo.getMime())).thenReturn("https://api.cloudinary.com/temporal");

        assertThat(service.obtener(curso.getId(), archivo.getId()).expiraEnSegundos()).isEqualTo(300);
    }

    @Test void documentoRawHistoricoConservaSuEnlaceOriginal() {
        var alumno = TestData.usuario("ESTUDIANTE"); TestData.autenticar(alumno);
        var curso = new Curso(); curso.setId(UUID.randomUUID());
        var archivo = imagen(curso.getId(), alumno.getId(), "ENTREGA");
        archivo.setResourceType("raw"); archivo.setMime("application/pdf");
        when(acceso.obtener(curso.getId())).thenReturn(curso);
        when(archivos.findById(archivo.getId())).thenReturn(Optional.of(archivo));
        var result = service.obtener(curso.getId(), archivo.getId());
        assertThat(result.url()).isEqualTo(archivo.getUrl());
        assertThat(result.expiraEnSegundos()).isZero();
        verifyNoInteractions(cloudinary);
    }

    private Archivo imagen(UUID cursoId, UUID usuarioId, String destino) {
        Archivo a = new Archivo(); a.setId(UUID.randomUUID()); a.setCursoId(cursoId); a.setUsuarioId(usuarioId);
        a.setProveedor("CLOUDINARY"); a.setDestino(destino); a.setNombre("imagen.png"); a.setMime("image/png");
        a.setPublicId("curso/imagen"); a.setResourceType("image"); a.setBytes(8); a.setUrl("https://res.cloudinary.com/firma-antigua");
        return a;
    }
}
