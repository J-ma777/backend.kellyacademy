package com.kellyacademy.shared.util;

import java.util.regex.Pattern;

/*
  Validador de URLs para campos de entidades del dominio academico.

  Acepta los esquemas usados por la plataforma:
    - http:// y https://  -> recursos web, materiales, instrucciones
    - rtmp:// y rtsp://   -> streaming en vivo de clases

  No valida que la URL exista, solo el formato. La existencia se valida
  cuando el usuario hace click (responsabilidad del frontend).
 */
public final class UrlValidator {

    private static final Pattern PATRON_URL =
            Pattern.compile("^(https?|rtmp|rtsp)://.+$", Pattern.CASE_INSENSITIVE);

    private UrlValidator() {
    }

    /*
      Devuelve true si la URL es null o blank (el llamador decide si eso es valido).
      Si viene con contenido, valida el formato.
     */
    public static boolean esFormatoValido(String url) {
        if (url == null || url.isBlank()) {
            return true;
        }
        return PATRON_URL.matcher(url.trim()).matches();
    }
}