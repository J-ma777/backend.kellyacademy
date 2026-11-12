package com.kellyacademy.security.clientip;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClientIpResolverTest {
    @Test
    void directIgnoresAllForwardedHeaders() {
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.40");
        request.addHeader("X-Forwarded-For", "203.0.113.1");
        request.addHeader("CF-Connecting-IP", "203.0.113.2");
        assertThat(new ClientIpResolver("direct").resolve(request)).contains("192.0.2.40");
    }

    @Test
    void renderUsesOnlyTheEdgeHeader() {
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.40");
        request.addHeader("X-Forwarded-For", "203.0.113.1, 198.51.100.1");
        request.addHeader("CF-Connecting-IP", "198.51.100.20");
        assertThat(new ClientIpResolver("render-public").resolve(request)).contains("198.51.100.20");
    }

    @Test
    void renderFailsClosedOnMissingOrAmbiguousHeader() {
        var resolver = new ClientIpResolver("render-public");
        var missing = new MockHttpServletRequest();
        missing.setRemoteAddr("192.0.2.40");
        missing.addHeader("X-Forwarded-For", "203.0.113.1");
        assertThat(resolver.resolve(missing)).isEmpty();
        var repeated = new MockHttpServletRequest();
        repeated.addHeader("CF-Connecting-IP", "198.51.100.20");
        repeated.addHeader("CF-Connecting-IP", "203.0.113.1");
        assertThat(resolver.resolve(repeated)).isEmpty();
    }

    @Test
    void rejectsNonLiteralAndMalformedAddresses() {
        var resolver = new ClientIpResolver("render-public");
        for (String invalid : new String[]{"", "example.com", "198.51.100.999", "01.2.3.4",
                "198.51.100.20, 203.0.113.1", "198.51.100.20\nforged", "[2001:db8::1]",
                "fe80::1%en0", " 198.51.100.20", "2001:db8::1:garbage"}) {
            var request = new MockHttpServletRequest();
            request.addHeader("CF-Connecting-IP", invalid);
            assertThat(resolver.resolve(request)).as("invalid address: %s", invalid).isEmpty();
        }
    }

    @Test
    void supportsIpv6AndRejectsUnknownMode() {
        var request = new MockHttpServletRequest();
        request.addHeader("CF-Connecting-IP", "2001:db8::1");
        assertThat(new ClientIpResolver("render-public").resolve(request)).contains("2001:db8:0:0:0:0:0:1");
        assertThatThrownBy(() -> new ClientIpResolver("unknown")).isInstanceOf(IllegalArgumentException.class);
    }
}
