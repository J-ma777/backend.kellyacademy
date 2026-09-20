package com.kellyacademy.shared.config;

import java.time.ZoneId;

/*
  Configuracion centralizada de tiempo de la aplicacion.

  Los timestamps de auditoria y errores se generan con zona explicita
  para evitar dependencia de la zona del sistema operativo del servidor.
  Esto garantiza consistencia cuando la app corre en multiples nodos
  con zonas distintas (dev local en America/Lima, prod en UTC, etc.).
 */
public final class AppTime {

    public static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/Lima");

    private AppTime() {
    }
}