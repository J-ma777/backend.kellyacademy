package com.kellyacademy.security.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.cors.allowed-origins=https://frontend.example, https://otro.example")
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
class CorsHttpIT {

    @Autowired private TestRestTemplate rest;

    @Test
    void preflightPermiteOrigenConfiguradoYHeadersDeAutenticacion() {
        HttpHeaders headers = new HttpHeaders();
        headers.setOrigin("https://frontend.example");
        headers.setAccessControlRequestMethod(HttpMethod.POST);
        headers.setAccessControlRequestHeaders(List.of("authorization", "content-type"));

        var response = rest.exchange("/api/cursos", HttpMethod.OPTIONS, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().getAccessControlAllowOrigin()).isEqualTo("https://frontend.example");
        assertThat(response.getHeaders().getAccessControlAllowHeaders().stream()
                .map(header -> header.toLowerCase(Locale.ROOT)).toList())
                .contains("authorization", "content-type");
    }

    @Test
    void preflightRechazaOrigenNoConfigurado() {
        HttpHeaders headers = new HttpHeaders();
        headers.setOrigin("https://desconocido.example");
        headers.setAccessControlRequestMethod(HttpMethod.POST);

        var response = rest.exchange("/api/cursos", HttpMethod.OPTIONS, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getHeaders().getAccessControlAllowOrigin()).isNull();
    }

    @Test
    void errorDeAutenticacionConservaCabecerasCors() {
        HttpHeaders headers = new HttpHeaders();
        headers.setOrigin("https://otro.example");

        var response = rest.exchange("/api/cursos", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getHeaders().getAccessControlAllowOrigin()).isEqualTo("https://otro.example");
    }
}
