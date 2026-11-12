package com.kellyacademy.storage;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.http.MediaType;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.net.URI;
import java.net.URLDecoder;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class CloudinaryGatewayTest {
    @Test void firmaSubidaEnServidorYDevuelveIdentificadorSinUrlPermanente() throws Exception {
        var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();
        var id=new AtomicReference<String>();
        server.expect(requestTo("https://api.cloudinary.com/v1_1/test-cloud/raw/upload"))
            .andExpect(request->{
                var body=((MockClientHttpRequest)request).getBodyAsString(StandardCharsets.UTF_8);
                id.set(field(body,"public_id"));
                assertThat(id.get()).endsWith(".pdf");assertThat(field(body,"type")).isEqualTo("authenticated");
                assertThat(field(body,"api_key")).isEqualTo("test-key");
                String payload="overwrite=false&public_id="+id.get()+"&timestamp="+field(body,"timestamp")+"&type=authenticatedtest-secret";
                try {
                    assertThat(field(body,"signature")).isEqualTo(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(payload.getBytes(StandardCharsets.UTF_8))));
                }catch(Exception e){throw new AssertionError(e);}
                assertThat(body).doesNotContain("test-secret");
            }).andRespond(request->withSuccess("{\"public_id\":\""+id.get()+"\",\"resource_type\":\"raw\"}",MediaType.APPLICATION_JSON).createResponse(request));
        var gateway=new CloudinaryGateway("test-cloud","test-key","test-secret",builder.build());
        var uploaded=gateway.upload("%PDF-1.7".getBytes(),"pdf","application/pdf",UUID.randomUUID());
        assertThat(uploaded.publicId()).isEqualTo(id.get());
        assertThat(uploaded.resourceType()).isEqualTo("raw");
        server.verify();
    }
    @Test void descargaDeImagenEsTemporalYLaFirmaCubreTodosLosParametros() throws Exception {
        var gateway = new CloudinaryGateway("test-cloud", "test-key", "test-secret", RestClient.builder().build());
        var ahora = Instant.parse("2026-11-11T12:00:00Z");
        String enlace = gateway.downloadUrl("kellyacademy/curso/imagen", "image/png", ahora);
        URI url = URI.create(enlace);
        assertThat(url.getHost()).isEqualTo("api.cloudinary.com");
        assertThat(url.getPath()).isEqualTo("/v1_1/test-cloud/image/download");
        var params = new TreeMap<String,String>();
        for (String param : url.getRawQuery().split("&")) {
            String[] par = param.split("=", 2);
            params.put(URLDecoder.decode(par[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(par[1], StandardCharsets.UTF_8));
        }
        assertThat(params.remove("api_key")).isEqualTo("test-key");
        String firma = params.remove("signature");
        assertThat(params).containsEntry("type", "authenticated").containsEntry("attachment", "false")
                .containsEntry("format", "png").containsEntry("public_id", "kellyacademy/curso/imagen")
                .containsEntry("timestamp", Long.toString(ahora.getEpochSecond()))
                .containsEntry("expires_at", Long.toString(ahora.plusSeconds(300).getEpochSecond()));
        String payload = params.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue())
                .collect(java.util.stream.Collectors.joining("&"));
        assertThat(firma).isEqualTo(HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-1").digest((payload+"test-secret").getBytes(StandardCharsets.UTF_8))));
        assertThat(enlace).doesNotContain("test-secret").doesNotContain("res.cloudinary.com");
    }

    @Test void informaCredencialesDeDistintoEntornoSinExponerRespuestaDelProveedor() {
        var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.cloudinary.com/v1_1/test-cloud/raw/upload"))
            .andRespond(withStatus(org.springframework.http.HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_JSON).body("{\"error\":{\"message\":\"cloud_name mismatch test-key\"}}"));
        var gateway=new CloudinaryGateway("test-cloud","test-key","test-secret",builder.build());
        assertThatThrownBy(()->gateway.upload("%PDF-1.7".getBytes(),"pdf","application/pdf",UUID.randomUUID()))
            .isInstanceOf(com.kellyacademy.shared.exception.BusinessException.class)
            .hasMessageContaining("mismo entorno").hasMessageNotContaining("test-key").hasMessageNotContaining("test-secret");
        server.verify();
    }
    private String field(String multipart,String name){
        int field=multipart.indexOf("name=\""+name+"\"");
        if(field<0)throw new AssertionError("Campo multipart ausente: "+name);
        int start=multipart.indexOf("\r\n\r\n",field)+4;
        int end=multipart.indexOf("\r\n",start);
        return multipart.substring(start,end);
    }
}
