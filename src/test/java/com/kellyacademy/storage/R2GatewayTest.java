package com.kellyacademy.storage;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class R2GatewayTest {
    @Test void enlacesTemporalesSeGeneranSinExponerElSecretoNiPersistirlos(){
        var gateway=new R2Gateway("https://aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa.r2.cloudflarestorage.com","test-key","test-secret","private-bucket","auto");
        try {
            String url=gateway.downloadUrl("cursos/curso/respuesta.pdf");
            assertThat(url).contains("X-Amz-Expires=300").contains("X-Amz-Signature=").contains("respuesta.pdf").doesNotContain("test-secret");
        }finally{gateway.close();}
    }
    @Test void backendPuedeArrancarSinR2YFallaConUnMensajeClaroAlSubir(){
        var gateway=new R2Gateway("","","","","auto");
        assertThatThrownBy(()->gateway.upload(new byte[]{1},"pdf","application/pdf",java.util.UUID.randomUUID())).hasMessageContaining("Configura R2");
    }
}
