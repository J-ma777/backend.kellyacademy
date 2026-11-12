package com.kellyacademy.security.bootstrap;

import com.kellyacademy.user.repository.UsuarioRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Secures the historical Flyway admin without modifying an applied migration. */
@Component
@Profile("prod")
public class InitialAdminCredentialGuard {
    private static final UUID INITIAL_ADMIN_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    // This credential is public in V10, and is used only to detect installations that still have it.
    private static final String KNOWN_INITIAL_PASSWORD = "Admin123*";

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transactions;
    private final String replacementPassword;

    public InitialAdminCredentialGuard(UsuarioRepository usuarios, PasswordEncoder passwordEncoder,
                                       PlatformTransactionManager transactionManager,
                                       @Value("${APP_BOOTSTRAP_ADMIN_PASSWORD:}") String replacementPassword) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.transactions = new TransactionTemplate(transactionManager);
        this.replacementPassword = replacementPassword;
    }

    @PostConstruct
    public void secureInitialAdmin() {
        // The bean is initialized before the embedded server starts accepting traffic.
        // Locking serializes concurrent instance startups against the same database.
        transactions.executeWithoutResult(status -> usuarios.findByIdForUpdate(INITIAL_ADMIN_ID).ifPresent(admin -> {
            if (!passwordEncoder.matches(KNOWN_INITIAL_PASSWORD, admin.getContrasena())) return;
            if (!isStrong(replacementPassword)) {
                throw new IllegalStateException("La cuenta administradora inicial conserva su contraseña conocida. "
                        + "Configura APP_BOOTSTRAP_ADMIN_PASSWORD con 12 a 72 bytes, mayúsculas, minúsculas, "
                        + "números y símbolos antes de iniciar producción.");
            }
            admin.setContrasena(passwordEncoder.encode(replacementPassword));
            usuarios.save(admin);
        }));
    }

    private static boolean isStrong(String password) {
        if (password == null || password.getBytes(StandardCharsets.UTF_8).length < 12
                || password.getBytes(StandardCharsets.UTF_8).length > 72
                || password.codePoints().anyMatch(Character::isWhitespace)) return false;
        return password.codePoints().anyMatch(Character::isUpperCase)
                && password.codePoints().anyMatch(Character::isLowerCase)
                && password.codePoints().anyMatch(Character::isDigit)
                && password.codePoints().anyMatch(c -> !Character.isLetterOrDigit(c));
    }
}
