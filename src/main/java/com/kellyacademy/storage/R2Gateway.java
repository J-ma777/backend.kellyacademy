package com.kellyacademy.storage;

import com.kellyacademy.shared.exception.BusinessException;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.*;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import java.net.URI;
import java.time.Duration;
import java.util.UUID;

@Component
public class R2Gateway {
    private final String endpoint,key,secret,bucket,region;
    private S3Client client;
    private S3Presigner presigner;
    public R2Gateway(@Value("${storage.r2.endpoint:}") String endpoint,
            @Value("${storage.r2.access-key-id:}") String key,@Value("${storage.r2.secret-access-key:}") String secret,
            @Value("${storage.r2.bucket:}") String bucket,@Value("${storage.r2.region:auto}") String region) {
        this.endpoint=endpoint;this.key=key;this.secret=secret;this.bucket=bucket;this.region=region;
    }
    private synchronized void configure() {
        if(client!=null)return;
        if(endpoint.isBlank()||key.isBlank()||secret.isBlank()||bucket.isBlank())
            throw new BusinessException("R2_PENDIENTE","Configura R2 en el backend para subir documentos");
        URI uri=URI.create(endpoint);
        if(!"https".equals(uri.getScheme())||uri.getHost()==null||!uri.getHost().endsWith(".r2.cloudflarestorage.com")||uri.getUserInfo()!=null)
            throw new BusinessException("R2_CONFIGURACION_INVALIDA","Revisa el endpoint S3 HTTPS de Cloudflare R2");
        var credentials=StaticCredentialsProvider.create(AwsBasicCredentials.create(key,secret));
        var config=S3Configuration.builder().chunkedEncodingEnabled(false).pathStyleAccessEnabled(true).build();
        client=S3Client.builder().endpointOverride(uri).region(Region.of(region)).credentialsProvider(credentials)
                .serviceConfiguration(config).requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .httpClientBuilder(UrlConnectionHttpClient.builder().connectionTimeout(Duration.ofSeconds(10)).socketTimeout(Duration.ofSeconds(60)))
                .build();
        presigner=S3Presigner.builder().endpointOverride(uri).region(Region.of(region)).credentialsProvider(credentials)
                .serviceConfiguration(config).build();
    }
    public String upload(byte[] data,String extension,String mime,UUID courseId) {
        configure();String object="cursos/"+courseId+"/"+UUID.randomUUID()+"."+extension;
        try {
            client.putObject(PutObjectRequest.builder().bucket(bucket).key(object).contentType(mime).build(),RequestBody.fromBytes(data));
            return object;
        }catch(S3Exception ex){throw error(ex);}
        catch(Exception ex){throw new BusinessException("R2_NO_DISPONIBLE","No se pudo conectar con R2. Vuelve a intentarlo");}
    }
    public String downloadUrl(String object) {
        configure();
        return presigner.presignGetObject(GetObjectPresignRequest.builder().signatureDuration(Duration.ofMinutes(5))
                .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(object).build()).build()).url().toString();
    }
    public byte[] read(String object) {
        configure();
        try(var response=client.getObject(GetObjectRequest.builder().bucket(bucket).key(object).build())){
            int max=20*1024*1024;
            if(response.response().contentLength()!=null&&response.response().contentLength()>max)
                throw new BusinessException("ARCHIVO_TAMANO","El documento supera el tamaño admitido para la vista previa");
            byte[] bytes=response.readNBytes(max+1);
            if(bytes.length>max)throw new BusinessException("ARCHIVO_TAMANO","El documento supera el tamaño admitido para la vista previa");
            return bytes;
        }catch(S3Exception ex){throw error(ex);}
        catch(java.io.IOException ex){throw new BusinessException("R2_NO_DISPONIBLE","No se pudo leer el documento. Vuelve a intentarlo");}
    }
    public void delete(String object) {
        configure();try{client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(object).build());}
        catch(S3Exception ex){throw error(ex);}
    }
    private BusinessException error(S3Exception ex){
        return ex.statusCode()==401||ex.statusCode()==403
                ?new BusinessException("R2_ACCESO_DENEGADO","R2 rechazó el acceso. Revisa las credenciales y el permiso Object Read & Write del bucket")
                :new BusinessException("R2_SUBIDA_FALLIDA","R2 no pudo procesar el documento. Revisa el bucket y vuelve a intentarlo");
    }
    @PreDestroy public void close(){if(client!=null)client.close();if(presigner!=null)presigner.close();}
}
