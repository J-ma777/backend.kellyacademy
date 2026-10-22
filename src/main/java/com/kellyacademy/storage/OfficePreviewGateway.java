package com.kellyacademy.storage;

import com.kellyacademy.shared.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.Semaphore;

@Component
public class OfficePreviewGateway {
    private final RestClient client;
    private final String endpoint;
    private final Semaphore capacity=new Semaphore(2);
    public OfficePreviewGateway(@Value("${storage.preview.converter-url:}") String endpoint){
        this.endpoint=endpoint;
        var factory=new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(65));client=RestClient.builder().requestFactory(factory).build();
    }
    public byte[] toPdf(byte[] original,String filename){
        if(endpoint.isBlank())throw new BusinessException("CONVERSOR_PENDIENTE","El visor de Office requiere iniciar el servicio de documentos");
        if(!capacity.tryAcquire())throw new BusinessException("VISTA_PREVIA_OCUPADA","El visor está procesando otros documentos. Vuelve a intentarlo en unos momentos");
        try {
            String extension=filename.substring(filename.lastIndexOf('.')+1).toLowerCase(java.util.Locale.ROOT);
            if(!java.util.Set.of("doc","docx","xls","xlsx","ppt","pptx","txt").contains(extension))
                throw new BusinessException("VISTA_PREVIA_NO_ADMITIDA","Este formato no admite vista previa");
            var body=new LinkedMultiValueMap<String,Object>();
            body.add("files",new ByteArrayResource(original){@Override public String getFilename(){return "documento."+extension;}});
            return client.post().uri(endpoint.replaceAll("/$", "")+"/forms/libreoffice/convert")
                    .contentType(MediaType.MULTIPART_FORM_DATA).body(body).exchange((request,response)->{
                        if(!response.getStatusCode().is2xxSuccessful())throw new BusinessException("CONVERSION_FALLIDA","No se pudo generar la vista previa. El documento puede estar dañado o protegido con contraseña");
                        int max=40*1024*1024;byte[] result=response.getBody().readNBytes(max+1);
                        if(result.length>max||result.length<5||!new String(result,0,5,java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-"))
                            throw new BusinessException("VISTA_PREVIA_INVALIDA","No se pudo generar una vista previa válida. Puedes descargar el original");
                        return result;
                    });
        }catch(BusinessException ex){throw ex;}
        catch(Exception ex){throw new BusinessException("CONVERSOR_NO_DISPONIBLE","El visor de Office no está disponible. Puedes reintentar o descargar el archivo original");}
        finally{capacity.release();}
    }
}
