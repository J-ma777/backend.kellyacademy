package com.kellyacademy.storage;

import com.kellyacademy.course.entity.Curso;
import com.kellyacademy.course.service.AccesoCursoService;
import com.kellyacademy.support.TestData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArchivoServiceImageTest {
    @Mock AccesoCursoService acceso;
    @Mock CloudinaryGateway cloudinary;
    @Mock R2Gateway r2;
    @Mock ArchivoRepository archivos;
    @InjectMocks ArchivoService service;

    @AfterEach void limpiar() { SecurityContextHolder.clearContext(); }

    @Test void subirImagenEntregaReferenciaPropiaSinFirmaDelProveedor() {
        var usuario = TestData.usuario("ESTUDIANTE"); TestData.autenticar(usuario);
        var curso = new Curso(); curso.setId(UUID.randomUUID());
        UUID archivoId = UUID.randomUUID();
        ReflectionTestUtils.setField(service, "publicUrl", "https://api.example.test");
        when(acceso.obtener(curso.getId())).thenReturn(curso);
        when(cloudinary.upload(any(), eq("png"), eq("image/png"), eq(curso.getId())))
                .thenReturn(new CloudinaryGateway.Uploaded("curso/imagen", "image"));
        when(archivos.save(any())).thenAnswer(inv -> {
            Archivo archivo = inv.getArgument(0); archivo.setId(archivoId); return archivo;
        });
        byte[] png = {(byte)137,80,78,71,13,10,26,10};
        var file = new MockMultipartFile("archivo", "respuesta.png", "image/png", png);

        var resultado = service.subir(curso.getId(), ArchivoService.Destino.ENTREGA, file);
        assertThat(resultado.url()).isEqualTo("https://api.example.test/api/cursos/" + curso.getId() + "/archivos/" + archivoId)
                .doesNotContain("cloudinary.com", "s--", "signature");
        verify(acceso).validarEstudianteActivo(curso);
    }
}
