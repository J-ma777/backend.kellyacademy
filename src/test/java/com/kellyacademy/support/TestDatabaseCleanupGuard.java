package com.kellyacademy.support;

/** Rejects non-disposable databases before the integration fixtures truncate tables. */
final class TestDatabaseCleanupGuard {
    private TestDatabaseCleanupGuard() {}

    static void verify(String engine, String url, String driver, boolean postgresSuite) {
        if ("H2".equals(engine) && url != null && url.startsWith("jdbc:h2:mem:")) return;
        if ("PostgreSQL".equals(engine) && postgresSuite && url != null
                && url.startsWith("jdbc:tc:postgresql:")
                && "org.testcontainers.jdbc.ContainerDatabaseDriver".equals(driver)) return;
        throw new IllegalStateException("Se rechaza la limpieza de una base que no es efímera de pruebas");
    }
}
