package com.kellyacademy.security.ratelimit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/** Fixed window shared between instances; database time and an atomic UPSERT serialize clients. */
@Service
@ConditionalOnProperty(name = "app.auth-rate-limit.enabled", havingValue = "true")
public class LoginRateLimiter {
    private final JdbcTemplate jdbc;
    private final int maxAttempts;
    private final int windowSeconds;
    private final LongSupplier nanoTime;
    // A bounded local cache only remembers denials already decided by PostgreSQL.
    // It does not grant requests or replace the shared authoritative counter.
    private final ConcurrentHashMap<String, Long> blockedUntil = new ConcurrentHashMap<>();
    private static final int MAX_CACHED_BLOCKS = 10_000;

    @Autowired
    public LoginRateLimiter(JdbcTemplate jdbc,
            @Value("${app.auth-rate-limit.max-attempts:120}") int maxAttempts,
            @Value("${app.auth-rate-limit.window-seconds:900}") int windowSeconds) {
        this(jdbc, maxAttempts, windowSeconds, System::nanoTime);
    }

    LoginRateLimiter(JdbcTemplate jdbc, int maxAttempts, int windowSeconds, LongSupplier nanoTime) {
        if (maxAttempts < 1 || maxAttempts > 1_000_000 || windowSeconds < 1 || windowSeconds > 86_400)
            throw new IllegalArgumentException("Configuración inválida del límite de autenticación");
        this.jdbc = jdbc;
        this.maxAttempts = maxAttempts;
        this.windowSeconds = windowSeconds;
        this.nanoTime = nanoTime;
    }

    public record Decision(boolean allowed, long retryAfterSeconds) {}

    public Decision check(String clientIp) {
        String key = hash(clientIp);
        long now = nanoTime.getAsLong();
        Long until = blockedUntil.get(key);
        if (until != null) {
            long remaining = until - now;
            if (remaining > 0) return new Decision(false, Math.max(1, (remaining + 999_999_999L) / 1_000_000_000L));
            blockedUntil.remove(key, until);
        }
        Decision result = jdbc.queryForObject("""
            INSERT INTO auth_rate_limits AS limits (client_key, window_started_at, attempts)
            VALUES (?, CURRENT_TIMESTAMP, 1)
            ON CONFLICT (client_key) DO UPDATE SET
              attempts = CASE WHEN limits.window_started_at + (? * INTERVAL '1 second') <= CURRENT_TIMESTAMP
                         THEN 1 ELSE LEAST(limits.attempts::bigint + 1, 2147483647)::integer END,
              window_started_at = CASE WHEN limits.window_started_at + (? * INTERVAL '1 second') <= CURRENT_TIMESTAMP
                                  THEN CURRENT_TIMESTAMP ELSE limits.window_started_at END
            RETURNING attempts, CEIL(EXTRACT(EPOCH FROM
                (window_started_at + (? * INTERVAL '1 second') - CURRENT_TIMESTAMP))) AS remaining
            """, (rs, row) -> new Decision(rs.getInt("attempts") <= maxAttempts,
                    Math.max(1L, rs.getLong("remaining"))),
                key, windowSeconds, windowSeconds, windowSeconds);
        if (result != null && !result.allowed()) {
            // Starting the deadline before the database call avoids extending a
            // block when obtaining a connection or row lock takes a long time.
            long deadline = now + TimeUnit.SECONDS.toNanos(result.retryAfterSeconds());
            long remaining = deadline - nanoTime.getAsLong();
            if (remaining <= 0) return new Decision(false, 1);
            synchronized (blockedUntil) {
                if (blockedUntil.size() < MAX_CACHED_BLOCKS || blockedUntil.containsKey(key)) {
                    blockedUntil.put(key, deadline);
                }
            }
            return new Decision(false, Math.max(1, (remaining + 999_999_999L) / 1_000_000_000L));
        }
        return result;
    }

    // Retain expired windows for at least a day, then remove them without touching other data.
    @Scheduled(fixedDelayString = "${app.auth-rate-limit.cleanup-ms:900000}")
    public void cleanup() {
        long now = nanoTime.getAsLong();
        blockedUntil.entrySet().removeIf(entry -> entry.getValue() - now <= 0);
        int retention = Math.max(86_400, windowSeconds * 4);
        jdbc.update("DELETE FROM auth_rate_limits WHERE window_started_at < CURRENT_TIMESTAMP - (? * INTERVAL '1 second')", retention);
    }

    private static String hash(String ip) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(ip.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
