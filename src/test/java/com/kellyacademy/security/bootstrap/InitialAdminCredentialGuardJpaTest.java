package com.kellyacademy.security.bootstrap;

import com.kellyacademy.user.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class InitialAdminCredentialGuardJpaTest {
    private static final UUID ADMIN_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    @Autowired private UsuarioRepository usuarios;
    @Autowired private PlatformTransactionManager transactions;
    @Autowired private EntityManager em;

    @Test void rotaLaCredencialEnBaseDeDatosSinAlterarElUsuario() {
        var encoder = new BCryptPasswordEncoder(4);
        em.createNativeQuery("""
                INSERT INTO usuarios (id,nombre,apellido,correo_electronico,contrasena,estado,fecha_creacion,fecha_actualizacion)
                VALUES (:id,'Admin','Inicial','admin-test@example.test',:hash,'ACTIVO',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
                """).setParameter("id", ADMIN_ID).setParameter("hash", encoder.encode("Admin123*")).executeUpdate();
        em.flush();
        em.clear();

        new InitialAdminCredentialGuard(usuarios, encoder, transactions, "NuevaClave#Segura123").secureInitialAdmin();
        em.flush();
        em.clear();

        var admin = usuarios.findById(ADMIN_ID).orElseThrow();
        assertThat(admin.getCorreoElectronico()).isEqualTo("admin-test@example.test");
        assertThat(encoder.matches("NuevaClave#Segura123", admin.getContrasena())).isTrue();
    }
}
