package com.kellyacademy.shared.util;

import com.kellyacademy.shared.exception.BusinessException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

public final class HtmlSeguro {
    private HtmlSeguro() {}
    public static String limpiar(String html) {
        String limpio = Jsoup.clean(html == null ? "" : html, "", Safelist.basic().addTags("u", "h3")
                .removeProtocols("a", "href", "ftp", "mailto").addEnforcedAttribute("a", "rel", "noopener noreferrer"),
                new Document.OutputSettings().prettyPrint(false));
        if (Jsoup.parse(limpio).text().isBlank()) throw new BusinessException("ANUNCIO_VACIO", "El anuncio debe contener texto");
        return limpio;
    }
}
