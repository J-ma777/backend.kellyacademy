package com.kellyacademy.security.clientip;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Resolves a literal IP only under an explicitly configured network trust boundary. */
@Component
public class ClientIpResolver {
    private static final String RENDER_HEADER = "CF-Connecting-IP";
    private final boolean renderPublic;

    public ClientIpResolver(@Value("${app.client-ip.mode:direct}") String mode) {
        this.renderPublic = switch (mode.toLowerCase(Locale.ROOT)) {
            case "direct" -> false;
            case "render-public" -> true;
            default -> throw new IllegalArgumentException("Invalid app.client-ip.mode; use direct or render-public");
        };
    }

    /** Empty means no trustworthy address; callers must handle this without using forwarded headers. */
    public Optional<String> resolve(HttpServletRequest request) {
        if (!renderPublic) {
            return literal(request.getRemoteAddr());
        }
        List<String> headers = Collections.list(request.getHeaders(RENDER_HEADER));
        if (headers.size() != 1) {
            return Optional.empty();
        }
        return literal(headers.get(0));
    }

    private Optional<String> literal(String value) {
        if (value == null || value.isBlank() || value.length() > 45 || !value.equals(value.trim())) {
            return Optional.empty();
        }
        if (value.indexOf(':') < 0) {
            String[] octets = value.split("\\.", -1);
            if (octets.length != 4) return Optional.empty();
            for (String octet : octets) {
                if (!octet.matches("0|[1-9][0-9]{0,2}") || Integer.parseInt(octet) > 255) {
                    return Optional.empty();
                }
            }
            return Optional.of(value);
        }
        // A colon and this alphabet restrict the input to IPv6 literals; no DNS lookup,
        // zone identifiers, ports, lists or control characters are permitted.
        if (!value.matches("[0-9a-fA-F:.]+")) return Optional.empty();
        try {
            return Optional.of(InetAddress.getByName(value).getHostAddress());
        } catch (UnknownHostException e) {
            return Optional.empty();
        }
    }
}
