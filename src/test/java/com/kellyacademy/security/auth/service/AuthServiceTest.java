package com.kellyacademy.security.auth.service;

import com.kellyacademy.security.auth.dto.AuthRequest;
import com.kellyacademy.security.jwt.JwtService;
import com.kellyacademy.security.user.CustomUserDetails;
import com.kellyacademy.support.TestData;
import com.kellyacademy.user.entity.Permiso;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock AuthenticationManager manager;
    @Mock JwtService jwt;
    @InjectMocks AuthService service;
    private AuthRequest request(String password) {
        var request = new AuthRequest(); request.setCorreoElectronico("ana@it.invalid"); request.setContrasena(password); return request;
    }
    @Test void autenticaAntesDeEmitirTokenYDevuelveRolesYPermisosReales() {
        var user = TestData.usuario("DOCENTE");
        var permiso = new Permiso(); permiso.setNombre("CURSO_CREAR");
        user.getRoles().iterator().next().setPermisos(Set.of(permiso));
        var details = new CustomUserDetails(user);
        when(manager.authenticate(any())).thenReturn(new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
        when(jwt.generarToken(details)).thenReturn("token-de-prueba");
        var result = service.login(request("PasswordTest123"));
        var credentials = ArgumentCaptor.forClass(Authentication.class); verify(manager).authenticate(credentials.capture());
        assertThat(credentials.getValue().getName()).isEqualTo("ana@it.invalid");
        assertThat(result.getAccessToken()).isEqualTo("token-de-prueba");
        assertThat(result.getTokenType()).isEqualTo("Bearer");
        assertThat(result.getUserId()).isEqualTo(user.getId());
        assertThat(result.getRoles()).containsExactly("DOCENTE");
        assertThat(result.getPermisos()).containsExactly("CURSO_CREAR");
    }
    @Test void credencialesIncorrectasNoProducenToken() {
        when(manager.authenticate(any())).thenThrow(new BadCredentialsException("Credenciales inválidas"));
        assertThatThrownBy(() -> service.login(request("incorrecta"))).isInstanceOf(BadCredentialsException.class);
        verifyNoInteractions(jwt);
    }
}
