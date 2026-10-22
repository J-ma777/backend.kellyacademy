package com.kellyacademy.storage;

import com.kellyacademy.shared.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;

@Component
public class CloudinaryGateway {
    private final String cloud,key,secret;
    private final RestClient client;
    public record Uploaded(String publicId,String resourceType,String url) {}
    @org.springframework.beans.factory.annotation.Autowired
    public CloudinaryGateway(@Value("${cloudinary.cloud-name:}") String cloud,
            @Value("${cloudinary.api-key:}") String key,@Value("${cloudinary.api-secret:}") String secret) {
        this(cloud,key,secret,configured(RestClient.builder()));
    }
    CloudinaryGateway(String cloud,String key,String secret,RestClient client) {
        this.cloud=cloud;this.key=key;this.secret=secret;this.client=client;
    }
    private static RestClient configured(RestClient.Builder builder) {
        var factory=new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
        factory.setReadTimeout(Duration.ofSeconds(60));
        return builder.requestFactory(factory).build();
    }
    public Uploaded upload(byte[] data,String extension,String mime,UUID courseId) {
        if(cloud.isBlank()||key.isBlank()||secret.isBlank())
            throw new BusinessException("CLOUDINARY_PENDIENTE","Configura Cloudinary en el backend para habilitar la subida de archivos");
        String resource=mime.startsWith("image/")?"image":"raw";
        String id="kellyacademy/"+courseId+"/"+UUID.randomUUID()+(resource.equals("raw")?"."+extension:"");
        var params=new TreeMap<String,String>();
        params.put("public_id",id);params.put("timestamp",Long.toString(Instant.now().getEpochSecond()));
        params.put("type","authenticated");params.put("overwrite","false");
        var body=new LinkedMultiValueMap<String,Object>();params.forEach(body::add);
        String payload=params.entrySet().stream().map(e->e.getKey()+"="+e.getValue()).collect(java.util.stream.Collectors.joining("&"));
        body.add("signature",HexFormat.of().formatHex(digest(payload+secret)));body.add("api_key",key);
        var file=new ByteArrayResource(data){@Override public String getFilename(){return "archivo."+extension;}};
        var headers=new HttpHeaders();headers.setContentType(MediaType.parseMediaType(mime));
        body.add("file",new HttpEntity<>(file,headers));
        try {
            Map<?,?> response=client.post().uri("https://api.cloudinary.com/v1_1/{cloud}/{resource}/upload",cloud,resource)
                    .contentType(MediaType.MULTIPART_FORM_DATA).body(body).retrieve().body(Map.class);
            if(response==null||!id.equals(response.get("public_id"))) throw new IllegalStateException("Respuesta inválida");
            String path=id+(resource.equals("image")?"."+response.get("format"):"");
            String signature=Base64.getUrlEncoder().withoutPadding().encodeToString(digest(path+secret)).substring(0,8);
            String url="https://res.cloudinary.com/"+cloud+"/"+resource+"/authenticated/s--"+signature+"--/"+path;
            return new Uploaded(id,resource,url);
        } catch(org.springframework.web.client.RestClientResponseException ex) {
            if(ex.getStatusCode().value()==401) {
                String message=ex.getResponseBodyAsString().contains("cloud_name mismatch")
                        ? "El Cloud name no corresponde a la API Key. Configura las tres credenciales del mismo entorno de Cloudinary"
                        : "Cloudinary rechazó las credenciales. Revisa el Cloud name, API Key y API Secret del mismo entorno";
                throw new BusinessException("CLOUDINARY_CREDENCIALES_INVALIDAS",message);
            }
            throw new BusinessException("SUBIDA_FALLIDA","Cloudinary rechazó el archivo. Revisa el formato, los límites y la configuración de la cuenta");
        } catch(Exception ex) {
            // No propagar respuestas del proveedor, claves ni firmas a los logs o al navegador.
            throw new BusinessException("SUBIDA_FALLIDA","No se pudo subir el archivo a Cloudinary. Revisa la configuración y vuelve a intentarlo");
        }
    }
    private static byte[] digest(String value) {
        try{return MessageDigest.getInstance("SHA-1").digest(value.getBytes(StandardCharsets.UTF_8));}
        catch(java.security.NoSuchAlgorithmException ex){throw new IllegalStateException(ex);}
    }
}
