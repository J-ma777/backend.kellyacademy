package com.kellyacademy.security.bootstrap;

import com.kellyacademy.user.entity.Usuario;
import com.kellyacademy.user.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InitialAdminCredentialGuardTest {
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final PlatformTransactionManager tx = mock(PlatformTransactionManager.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final UUID id = UUID.fromString("20000000-0000-0000-0000-000000000001");

    @BeforeEach void preparar() {
        when(tx.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    }

    @Test void identificaHashInicialDeLaMigracion() throws Exception {
        try (var stream = getClass().getResourceAsStream("/db/migration/V10__seed_base_data.sql")) {
            String sql = new String(java.util.Objects.requireNonNull(stream).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            var hashes = java.util.regex.Pattern.compile("\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}").matcher(sql);
            assertThat(hashes.find()).isTrue();
            Usuario usuario = new Usuario();
            usuario.setContrasena(hashes.group());
            when(usuarios.findByIdForUpdate(id)).thenReturn(Optional.of(usuario));
            new InitialAdminCredentialGuard(usuarios, encoder, tx, "ContrasenaSegura#123").secureInitialAdmin();
            assertThat(encoder.matches("ContrasenaSegura#123", usuario.getContrasena())).isTrue();
        }
    }

    @Test void reemplazaSoloLaContrasenaInicialConocida() {
        Usuario usuario = new Usuario();
        usuario.setContrasena(encoder.encode("Admin123*"));
        when(usuarios.findByIdForUpdate(id)).thenReturn(Optional.of(usuario));
        new InitialAdminCredentialGuard(usuarios, encoder, tx, "ContrasenaSegura#123").secureInitialAdmin();
        assertThat(encoder.matches("ContrasenaSegura#123", usuario.getContrasena())).isTrue();
        verify(usuarios).save(usuario);
        verify(tx).commit(any());
    }

    @Test void noAlteraContrasenaYaCambiadaNiExigeVariable() {
        Usuario usuario = new Usuario();
        String anterior = encoder.encode("OtroValor#Segur0");
        usuario.setContrasena(anterior);
        when(usuarios.findByIdForUpdate(id)).thenReturn(Optional.of(usuario));
        new InitialAdminCredentialGuard(usuarios, encoder, tx, "").secureInitialAdmin();
        assertThat(usuario.getContrasena()).isEqualTo(anterior);
        verify(usuarios, never()).save(any(Usuario.class));
    }

    @Test void noHaceNadaSiCuentaInicialNoExiste() {
        when(usuarios.findByIdForUpdate(id)).thenReturn(Optional.empty());
        new InitialAdminCredentialGuard(usuarios, encoder, tx, "").secureInitialAdmin();
        verify(usuarios, never()).save(any(Usuario.class));
    }

    @Test void sinClaveRobustaFallaSinGuardarNiMostrarSuValor() {
        Usuario usuario = new Usuario();
        usuario.setContrasena(encoder.encode("Admin123*"));
        when(usuarios.findByIdForUpdate(id)).thenReturn(Optional.of(usuario));
        String debil = "clave";
        assertThatThrownBy(() -> new InitialAdminCredentialGuard(usuarios, encoder, tx, debil).secureInitialAdmin())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_BOOTSTRAP_ADMIN_PASSWORD")
                .hasMessageNotContaining(debil);
        verify(usuarios, never()).save(any(Usuario.class));
        verify(tx).rollback(any());
    }
}
