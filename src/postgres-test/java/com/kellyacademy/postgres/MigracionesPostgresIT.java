package com.kellyacademy.postgres;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.DriverManager;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** Runs only with -Ppostgres-it, against a disposable PostgreSQL container. */
class MigracionesPostgresIT {
    @Test
    void todasLasMigracionesSeAplicanYLaNotaAdmiteCien() throws Exception {
        try (var postgres = new PostgreSQLContainer("postgres:18-alpine")) {
            postgres.start();
            var flyway = Flyway.configure()
                    .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .locations("classpath:db/migration")
                    .load();
            assertThat(flyway.migrate().migrationsExecuted).isGreaterThan(0);
            flyway.validate();
            assertThat(flyway.info().pending()).isEmpty();

            try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                 var query = connection.prepareStatement("""
                         SELECT numeric_precision, numeric_scale
                         FROM information_schema.columns
                         WHERE table_schema = current_schema()
                           AND table_name = 'matriculas' AND column_name = 'nota_final'
                         """);
                 var result = query.executeQuery()) {
                assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("PostgreSQL");
                assertThat(result.next()).isTrue();
                assertThat(result.getInt("numeric_precision")).isEqualTo(5);
                assertThat(result.getInt("numeric_scale")).isEqualTo(2);
            }

            String document = Files.readString(Path.of("docs/INVENTARIO-IMAGENES-HISTORICAS.md"));
            String inventario = document.substring(document.indexOf("WITH antiguas AS ("), document.indexOf("\n\nROLLBACK;"));
            try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                 var statement = connection.prepareStatement(inventario);
                 var result = statement.executeQuery()) {
                int locations = 0;
                while (result.next()) { assertThat(result.getLong("cantidad")).isZero(); locations++; }
                assertThat(locations).isEqualTo(7);
            }
        }
    }
}
