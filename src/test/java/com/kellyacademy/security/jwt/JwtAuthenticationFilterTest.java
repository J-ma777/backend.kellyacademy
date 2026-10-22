package com.kellyacademy.security.jwt;

import com.kellyacademy.security.user.*;
import com.kellyacademy.support.TestData;
import com.kellyacademy.user.enums.EstadoUsuario;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import jakarta.servlet.FilterChain;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {
    @Mock JwtService jwt;
    @Mock CustomUserDetailsService users;
    @Mock FilterChain chain;
    private JwtAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    @BeforeEach void setup() {
        filter = new JwtAuthenticationFilter(jwt, users, new ObjectMapper());
        request = new MockHttpServletRequest("GET", "/api/cursos"); response = new MockHttpServletResponse();
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    private void bearer() { request.addHeader("Authorization", "Bearer token-de-prueba"); }
    @Test void peticionSinBearerContinuaSinInventarSesion() throws Exception {
        filter.doFilter(request, response, chain); verify(chain).doFilter(request, response); verifyNoInteractions(jwt, users);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
    @Test void tokenValidoEstablecePrincipalYAutoridades() throws Exception {
        bearer(); var details = new CustomUserDetails(TestData.usuario("ESTUDIANTE"));
        when(jwt.extraerNombreUsuario("token-de-prueba")).thenReturn(details.getUsername());
        when(users.loadUserByUsername(details.getUsername())).thenReturn(details);
        when(jwt.esTokenValido("token-de-prueba", details)).thenReturn(true);
        filter.doFilter(request, response, chain);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isSameAs(details);
        verify(chain).doFilter(request, response);
    }
    @ParameterizedTest @EnumSource(value = EstadoUsuario.class, names = {"INACTIVO", "BLOQUEADO"})
    void bloqueaSesionesDeCuentasDeshabilitadas(EstadoUsuario estado) throws Exception {
        bearer(); var user = TestData.usuario("ESTUDIANTE"); user.setEstado(estado); var details = new CustomUserDetails(user);
        when(jwt.extraerNombreUsuario("token-de-prueba")).thenReturn(details.getUsername());
        when(users.loadUserByUsername(details.getUsername())).thenReturn(details);
        filter.doFilter(request, response, chain);
        assertThat(response.getStatus()).isEqualTo(401); assertThat(response.getContentAsString()).contains("ACCOUNT_DISABLED");
        verifyNoInteractions(chain); verify(jwt, never()).esTokenValido(anyString(), any());
    }
    @Test void tokenManipuladoResponde401YSinFiltrarDetallesInternos() throws Exception {
        bearer(); when(jwt.extraerNombreUsuario(anyString())).thenThrow(new JwtException("detalle privado"));
        filter.doFilter(request, response, chain);
        assertThat(response.getStatus()).isEqualTo(401); assertThat(response.getContentAsString()).contains("INVALID_TOKEN").doesNotContain("detalle privado", "token-de-prueba");
        verifyNoInteractions(chain);
    }
    @Test void usuarioEliminadoNoConservaAccesoConSuTokenAnterior() throws Exception {
        bearer(); when(jwt.extraerNombreUsuario(anyString())).thenReturn("eliminado@it.invalid");
        when(users.loadUserByUsername(anyString())).thenThrow(new UsernameNotFoundException("Eliminado"));
        filter.doFilter(request, response, chain);
        assertThat(response.getStatus()).isEqualTo(401); assertThat(response.getContentAsString()).contains("INVALID_SESSION");
        verifyNoInteractions(chain);
    }
}
