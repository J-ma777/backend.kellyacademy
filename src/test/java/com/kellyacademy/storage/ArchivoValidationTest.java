package com.kellyacademy.storage;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.util.zip.*;
import static org.assertj.core.api.Assertions.*;
class ArchivoValidationTest {
    @Test void compruebaFormatoOfficeRealYRechazaZipRenombrado() throws Exception {
        var output=new ByteArrayOutputStream();
        try(var zip=new ZipOutputStream(output)){
            zip.putNextEntry(new ZipEntry("[Content_Types].xml"));zip.write("tipos".getBytes());zip.closeEntry();
            zip.putNextEntry(new ZipEntry("word/document.xml"));zip.write("documento".getBytes());zip.closeEntry();
        }
        assertThat(ArchivoService.validar(output.toByteArray(),"docx")).contains("wordprocessingml");
        assertThatThrownBy(()->ArchivoService.validar(output.toByteArray(),"xlsx")).hasMessageContaining("Archivo inválido");
    }
    @Test void permiteImagenYBloqueaSvgYArchivoBinarioRenombrado(){
        assertThat(ArchivoService.validar(new byte[]{(byte)137,80,78,71,13,10,26,10},"png")).isEqualTo("image/png");
        assertThatThrownBy(()->ArchivoService.validar("<svg/>".getBytes(),"svg")).hasMessageContaining("Archivo inválido");
        assertThatThrownBy(()->ArchivoService.validar(new byte[]{0,1,2},"txt")).hasMessageContaining("Archivo inválido");
    }
    @Test void cloudinarySinCredencialesNoIntentaSolicitudExterna(){
        var gateway=new CloudinaryGateway("","","");
        assertThatThrownBy(()->gateway.upload("%PDF-".getBytes(),"pdf","application/pdf",java.util.UUID.randomUUID()))
            .hasMessageContaining("Configura Cloudinary");
    }
}
