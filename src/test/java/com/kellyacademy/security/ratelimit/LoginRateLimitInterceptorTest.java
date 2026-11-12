package com.kellyacademy.security.ratelimit;

import com.kellyacademy.security.clientip.ClientIpResolver;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class LoginRateLimitInterceptorTest {
    private final ClientIpResolver resolver = mock(ClientIpResolver.class);
    private final LoginRateLimiter limiter = mock(LoginRateLimiter.class);
    private final LoginRateLimitInterceptor interceptor = new LoginRateLimitInterceptor(resolver, limiter);

    @Test void bloqueaCon429YRetryAfterSinAfectarPreflight() throws Exception {
        var request = new MockHttpServletRequest("POST", "/auth/login");
        var response = new MockHttpServletResponse();
        when(resolver.resolve(request)).thenReturn(Optional.of("198.51.100.7"));
        when(limiter.check("198.51.100.7")).thenReturn(new LoginRateLimiter.Decision(false, 35));
        assertThat(interceptor.preHandle(request, response, new Object())).isFalse();
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("35");
        assertThat(response.getContentAsString()).contains("RATE_LIMITED").doesNotContain("198.51.100.7");
        assertThat(interceptor.preHandle(new MockHttpServletRequest("OPTIONS", "/auth/login"), new MockHttpServletResponse(), new Object())).isTrue();
    }

    @Test void unaIpDesconocidaOUnAlmacenCaidoNoDeshabilitanLaProteccion() throws Exception {
        var request = new MockHttpServletRequest("POST", "/auth/login");
        var response = new MockHttpServletResponse();
        when(resolver.resolve(request)).thenReturn(Optional.empty());
        assertThat(interceptor.preHandle(request, response, new Object())).isFalse();
        assertThat(response.getStatus()).isEqualTo(503);
        verifyNoInteractions(limiter);
        when(resolver.resolve(request)).thenReturn(Optional.of("198.51.100.7"));
        when(limiter.check(anyString())).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("fallo"));
        var unavailable = new MockHttpServletResponse();
        assertThat(interceptor.preHandle(request, unavailable, new Object())).isFalse();
        assertThat(unavailable.getStatus()).isEqualTo(503);
    }
}
