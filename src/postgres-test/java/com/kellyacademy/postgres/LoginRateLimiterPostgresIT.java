package com.kellyacademy.postgres;

import com.kellyacademy.security.ratelimit.LoginRateLimiter;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.concurrent.Executors;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class LoginRateLimiterPostgresIT {
    @Test void limitaAtomicamenteEntreInstanciasYRestableceLaVentana() throws Exception {
        try (var postgres = new PostgreSQLContainer("postgres:18-alpine")) {
            postgres.start();
            Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()).load().migrate();
            var source = new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
            var jdbc = new JdbcTemplate(source);
            var first = new LoginRateLimiter(jdbc, 3, 60);
            var second = new LoginRateLimiter(jdbc, 3, 60);
            var ready = new CountDownLatch(8);
            var start = new CountDownLatch(1);
            try (var pool = Executors.newFixedThreadPool(8)) {
                var results = IntStream.range(0, 8).mapToObj(i -> pool.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Sin señal");
                    return (i % 2 == 0 ? first : second).check("198.51.100.7");
                })).toList();
                assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
                start.countDown();
                int allowed = 0;
                for (var result : results) if (result.get(30, TimeUnit.SECONDS).allowed()) allowed++;
                assertThat(allowed).isEqualTo(3);
            }
            // A lower threshold must not truncate the shared counter and cause
            // another instance with a higher threshold to admit excess requests.
            assertThat(new LoginRateLimiter(jdbc, 5, 60).check("198.51.100.7").allowed()).isFalse();
            // Repeated denied requests do not wait for the database row once this
            // instance has received a denial from the shared counter.
            try (var locked = source.getConnection(); var pool = Executors.newSingleThreadExecutor()) {
                locked.setAutoCommit(false);
                try (var statement = locked.createStatement()) {
                    statement.executeQuery("SELECT client_key FROM auth_rate_limits FOR UPDATE");
                }
                try {
                    assertThat(pool.submit(() -> first.check("198.51.100.7").allowed()).get(2, TimeUnit.SECONDS)).isFalse();
                } finally {
                    locked.rollback();
                }
            }
            assertThat(first.check("198.51.100.8").allowed()).isTrue();
            jdbc.update("UPDATE auth_rate_limits SET window_started_at = CURRENT_TIMESTAMP - INTERVAL '61 seconds'");
            assertThat(new LoginRateLimiter(jdbc, 3, 60).check("198.51.100.7").allowed()).isTrue();
            jdbc.update("UPDATE auth_rate_limits SET window_started_at = CURRENT_TIMESTAMP - INTERVAL '2 days'");
            first.cleanup();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM auth_rate_limits", Integer.class)).isZero();
        }
    }
}
