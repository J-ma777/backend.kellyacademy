package com.kellyacademy.shared.util;
import com.kellyacademy.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

class HtmlSeguroTest {
    @Test void conservaFormatoPedagogicoYEliminaScriptsYAtributosEjecutables() {
        String clean = HtmlSeguro.limpiar("<h3>Instrucciones</h3><p><strong>Lee</strong> <u>todo</u></p><script>alert(1)</script><img src=x onerror=alert(1)><a href='javascript:alert(1)' onclick='alert(1)'>Enlace</a>");
        assertThat(clean).contains("<h3>", "<strong>", "<u>", "Enlace").doesNotContain("script", "onerror", "onclick", "javascript:");
    }
    @Test void enlacesWebConservanDestinoYProteccionContraOpener() {
        assertThat(HtmlSeguro.limpiar("<a href='https://example.org/guia'>Guía</a>")).contains("https://example.org/guia", "noopener noreferrer");
    }
    @ParameterizedTest @NullAndEmptySource @ValueSource(strings = {"   ", "<p><br></p>", "<script>alert(1)</script>", "<img src=x>"})
    void rechazaAnunciosSinTextoVisible(String html) {
        assertThatThrownBy(() -> HtmlSeguro.limpiar(html)).isInstanceOf(BusinessException.class);
    }
}
