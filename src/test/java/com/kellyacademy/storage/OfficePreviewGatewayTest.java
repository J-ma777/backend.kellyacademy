package com.kellyacademy.storage;

import com.kellyacademy.shared.exception.BusinessException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;

class OfficePreviewGatewayTest {
    @Test void enviaArchivoPorMultipartYValidaLaRespuestaPdf() throws Exception {
        var body=new AtomicReference<String>();var contentType=new AtomicReference<String>();
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/forms/libreoffice/convert",exchange->{
            body.set(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));
            contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            byte[] pdf="%PDF-1.7\nconvertido".getBytes();
            exchange.sendResponseHeaders(200,pdf.length);exchange.getResponseBody().write(pdf);exchange.close();
        });server.start();
        try {
            var gateway=new OfficePreviewGateway("http://127.0.0.1:"+server.getAddress().getPort()+"/");
            assertThat(gateway.toPdf("original privado".getBytes(),"Guía del curso.docx")).startsWith("%PDF-".getBytes());
            assertThat(contentType.get()).startsWith("multipart/form-data");
            assertThat(body.get()).contains("name=\"files\"", "filename=\"documento.docx\"", "original privado");
        }finally{server.stop(0);}
    }
    @Test void unaRespuestaQueNoEsPdfNoSeExponeComoVistaPrevia() throws Exception {
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/forms/libreoffice/convert",exchange->{byte[] error="<html>error</html>".getBytes();exchange.sendResponseHeaders(200,error.length);exchange.getResponseBody().write(error);exchange.close();});server.start();
        try { assertThatThrownBy(()->new OfficePreviewGateway("http://127.0.0.1:"+server.getAddress().getPort()).toPdf(new byte[]{1},"hoja.xlsx")).isInstanceOf(BusinessException.class).hasMessageContaining("vista previa válida"); }
        finally{server.stop(0);}
    }
    @Test void sinServicioConfiguradoInformaComoResolverlo(){
        assertThatThrownBy(()->new OfficePreviewGateway("").toPdf(new byte[]{1},"slides.pptx")).isInstanceOf(BusinessException.class).hasMessageContaining("iniciar el servicio");
    }
}
