package com.kellyacademy.postgres;

import com.kellyacademy.LmsBackendApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.DriverManager;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Verifies real Flyway, JPA validation and the production bootstrap against an isolated database. */
class ArranqueProduccionPostgresIT {
    @Test
    void iniciaConMigracionesYRotacionDelAdministradorInicial() throws Exception {
        String claveSintetica = "SoloParaPrueba#9876";
        try (var postgres = new PostgreSQLContainer("postgres:18-alpine")) {
            postgres.start();
            try (var context = iniciar(postgres, claveSintetica)) {
                assertThat(context.isActive()).isTrue();
                try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                     var query = connection.prepareStatement("SELECT contrasena FROM usuarios WHERE id = ?")) {
                    query.setObject(1, java.util.UUID.fromString("20000000-0000-0000-0000-000000000001"));
                    try (var result = query.executeQuery()) {
                        assertThat(result.next()).isTrue();
                        assertThat(new BCryptPasswordEncoder().matches(claveSintetica, result.getString(1))).isTrue();
                    }
                }
                int port = ((WebServerApplicationContext) context).getWebServer().getPort();
                var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/auth/login"))
                        .timeout(Duration.ofSeconds(10))
                        .header("Content-Type", "application/json")
                        .header("Origin", "http://localhost:3000")
                        .POST(HttpRequest.BodyPublishers.ofString("{\"correoElectronico\":\"admin@kellyacademy.com\",\"contrasena\":\"" + claveSintetica + "\"}"))
                        .build();
                var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
                assertThat(response.statusCode()).isEqualTo(200);
                assertThat(response.body()).contains("accessToken");
                var segunda = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
                assertThat(segunda.statusCode()).isEqualTo(200);
                var limitada = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
                assertThat(limitada.statusCode()).isEqualTo(429);
                assertThat(limitada.headers().firstValue("Retry-After")).isPresent();
                assertThat(limitada.headers().firstValue("Access-Control-Allow-Origin")).contains("http://localhost:3000");
                assertThat(limitada.headers().firstValue("Access-Control-Expose-Headers").orElse("")).containsIgnoringCase("Retry-After");
                assertThat(limitada.body()).contains("RATE_LIMITED");
            }
        }
    }
    @Test
    void sinCredencialInicialRobustaElArranqueSeRechaza() throws Exception {
        try (var postgres = new PostgreSQLContainer("postgres:18-alpine")) {
            postgres.start();
            assertThatThrownBy(() -> iniciar(postgres, ""))
                    .hasRootCauseInstanceOf(IllegalStateException.class)
                    .hasStackTraceContaining("APP_BOOTSTRAP_ADMIN_PASSWORD");
            try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                 var query = connection.prepareStatement("SELECT contrasena FROM usuarios WHERE id = ?")) {
                query.setObject(1, java.util.UUID.fromString("20000000-0000-0000-0000-000000000001"));
                try (var result = query.executeQuery()) {
                    assertThat(result.next()).isTrue();
                    assertThat(new BCryptPasswordEncoder().matches("Admin123*", result.getString(1))).isTrue();
                }
            }
        }
    }

    @Test
    void modoRenderNoConfiaEnForwardedForSiFaltaIpDelBorde() throws Exception {
        String clave = "SoloParaPrueba#9876";
        try (var postgres = new PostgreSQLContainer("postgres:18-alpine")) {
            postgres.start();
            try (var context = iniciar(postgres, clave, "render-public")) {
                int port = ((WebServerApplicationContext) context).getWebServer().getPort();
                var uri = URI.create("http://127.0.0.1:" + port + "/auth/login");
                String body = "{\"correoElectronico\":\"admin@kellyacademy.com\",\"contrasena\":\"" + clave + "\"}";
                var noVerificada = HttpRequest.newBuilder(uri).header("Content-Type", "application/json")
                        .header("X-Forwarded-For", "198.51.100.1")
                        .POST(HttpRequest.BodyPublishers.ofString(body)).build();
                var response = HttpClient.newHttpClient().send(noVerificada, HttpResponse.BodyHandlers.ofString());
                assertThat(response.statusCode()).isEqualTo(503);
                var verificada = HttpRequest.newBuilder(uri).header("Content-Type", "application/json")
                        .header("CF-Connecting-IP", "198.51.100.7")
                        .POST(HttpRequest.BodyPublishers.ofString(body)).build();
                assertThat(HttpClient.newHttpClient().send(verificada, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(200);
            }
        }
    }

    private org.springframework.context.ConfigurableApplicationContext iniciar(PostgreSQLContainer postgres, String clave) {
        return iniciar(postgres, clave, "direct");
    }

    private org.springframework.context.ConfigurableApplicationContext iniciar(PostgreSQLContainer postgres, String clave, String modo) {
        return new SpringApplicationBuilder(LmsBackendApplication.class).run(
                "--spring.profiles.active=prod",
                "--app.client-ip.mode=" + modo,
                "--app.auth-rate-limit.max-attempts=2",
                "--server.port=0",
                "--spring.datasource.url=" + postgres.getJdbcUrl(),
                "--spring.datasource.username=" + postgres.getUsername(),
                "--spring.datasource.password=" + postgres.getPassword(),
                "--spring.flyway.url=" + postgres.getJdbcUrl(),
                "--spring.flyway.user=" + postgres.getUsername(),
                "--spring.flyway.password=" + postgres.getPassword(),
                "--jwt.secret=VGhpc0lzQVRlc3RTZWNyZXRLZXlGb3JLZWxseUFjYWRlbXlMTVNCYWNrZW5kMjAyNlRlc3RpbmdPbmx5",
                "--jwt.expiration=3600000",
                "--APP_BOOTSTRAP_ADMIN_PASSWORD=" + clave
        );
    }

}
