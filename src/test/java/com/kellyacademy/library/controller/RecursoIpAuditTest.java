package com.kellyacademy.library.controller;

import com.kellyacademy.library.service.RecursoBibliotecaService;
import com.kellyacademy.security.clientip.ClientIpResolver;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import java.util.UUID;
import static org.mockito.Mockito.*;

class RecursoIpAuditTest {
    @Test void unaIpNoVerificadaSeRegistraComoDesconocida() {
        var service = mock(RecursoBibliotecaService.class);
        var controller = new RecursoBibliotecaController(service, new ClientIpResolver("render-public"));
        var request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "198.51.100.5");
        var id = UUID.randomUUID();
        controller.descargar(id, request);
        verify(service).descargar(id, null);
    }

    @Test void modoRenderRegistraLaIpDelBordeYNoLaCabeceraManipulada() {
        var service = mock(RecursoBibliotecaService.class);
        var controller = new RecursoBibliotecaController(service, new ClientIpResolver("render-public"));
        var request = new MockHttpServletRequest();
        request.addHeader("CF-Connecting-IP", "198.51.100.7");
        request.addHeader("X-Forwarded-For", "203.0.113.20");
        var id = UUID.randomUUID();
        controller.descargar(id, request);
        verify(service).descargar(id, "198.51.100.7");
    }
}
