package com.kellyacademy.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfig {
    // Strength 12: ~250-400ms por hash en hardware moderno. Imperceptible en login,
    // costoso para brute force. OWASP recomienda >= 10; 12 es el balance actual.
    // Cambiar strength no invalida hashes previos: va embebido en el propio hash.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
