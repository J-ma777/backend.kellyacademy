package com.kellyacademy.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TestDatabaseCleanupGuardTest {
    private static final String TC_DRIVER = "org.testcontainers.jdbc.ContainerDatabaseDriver";
    private static final String TC_URL = "jdbc:tc:postgresql:18-alpine:///kellyacademy?TC_DAEMON=true";

    @Test void permiteH2EnMemoria() {
        assertThatCode(() -> TestDatabaseCleanupGuard.verify("H2", "jdbc:h2:mem:testdb", "org.h2.Driver", false)).doesNotThrowAnyException();
    }
    @Test void rechazaH2EnDisco() {
        assertThatThrownBy(() -> TestDatabaseCleanupGuard.verify("H2", "jdbc:h2:file:./testdb", "org.h2.Driver", false)).isInstanceOf(IllegalStateException.class);
    }
    @Test void permitePostgresSoloConPerfilYTestcontainers() {
        assertThatCode(() -> TestDatabaseCleanupGuard.verify("PostgreSQL", TC_URL, TC_DRIVER, true)).doesNotThrowAnyException();
    }
    @Test void rechazaPostgresSiNoEstaActivoElPerfil() {
        assertThatThrownBy(() -> TestDatabaseCleanupGuard.verify("PostgreSQL", TC_URL, TC_DRIVER, false)).isInstanceOf(IllegalStateException.class);
    }
    @Test void rechazaPostgresExternoInclusoConPerfilActivo() {
        assertThatThrownBy(() -> TestDatabaseCleanupGuard.verify("PostgreSQL", "jdbc:postgresql://localhost:5432/real", "org.postgresql.Driver", true)).isInstanceOf(IllegalStateException.class);
    }
    @Test void rechazaDriverPostgresDirectoAunqueLaUrlParezcaTestcontainers() {
        assertThatThrownBy(() -> TestDatabaseCleanupGuard.verify("PostgreSQL", TC_URL, "org.postgresql.Driver", true)).isInstanceOf(IllegalStateException.class);
    }
}
