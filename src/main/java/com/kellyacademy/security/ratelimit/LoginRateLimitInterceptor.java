package com.kellyacademy.security.ratelimit;

import com.kellyacademy.security.clientip.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.auth-rate-limit.enabled", havingValue = "true")
public class LoginRateLimitInterceptor implements HandlerInterceptor {
    private final ClientIpResolver clientIp;
    private final LoginRateLimiter limiter;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if (!"POST".equalsIgnoreCase(request.getMethod())) return true;
        var ip = clientIp.resolve(request);
        if (ip.isEmpty()) {
            error(response, 503, "RATE_LIMIT_UNAVAILABLE", "No se pudo verificar la conexión. Inténtalo más tarde.");
            return false;
        }
        try {
            var decision = limiter.check(ip.get());
            if (decision.allowed()) return true;
            response.setHeader("Retry-After", Long.toString(decision.retryAfterSeconds()));
            error(response, 429, "RATE_LIMITED", "Demasiados intentos. Espera antes de volver a ingresar.");
        } catch (org.springframework.dao.DataAccessException ex) {
            // Do not let a failed store silently disable the limit or reveal client data.
            log.error("No se pudo consultar el límite de autenticación");
            error(response, 503, "RATE_LIMIT_UNAVAILABLE", "No se pudo verificar la conexión. Inténtalo más tarde.");
        }
        return false;
    }

    private static void error(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write("{\"codigo\":\"" + code + "\",\"mensaje\":\"" + message + "\",\"estado\":" + status + "}");
    }
}
