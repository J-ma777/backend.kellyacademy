package com.kellyacademy.shared.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(
        name = "Bearer Authentication",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
public class OpenApiConfig {

    @Bean
    public OpenAPI kellyAcademyOpenAPI() {

        Contact contacto = new Contact()
                .name("Kelly Academy")
                .email("soporte@kellyacademy.com");

        License licencia = new License()
                .name("Propietario")
                .url("https://kellyacademy.com/licencia");

        Info info = new Info()
                .title("Kelly Academy LMS API")
                .version("v1")
                .description("API REST del sistema LMS de Kelly Academy - English Online")
                .contact(contacto)
                .license(licencia);

        return new OpenAPI().info(info);
    }
}