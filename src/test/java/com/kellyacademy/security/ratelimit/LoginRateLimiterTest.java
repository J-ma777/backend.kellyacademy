package com.kellyacademy.security.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class LoginRateLimiterTest {
    @Test void unaConsultaDemoradaNoProlongaElBloqueoEnLaCache() {
        AtomicLong now = new AtomicLong();
        class SlowJdbc extends JdbcTemplate {
            int calls;
            @Override @SuppressWarnings("unchecked")
            public <T> T queryForObject(String sql, RowMapper<T> mapper, Object... args) {
                calls++;
                now.addAndGet(TimeUnit.SECONDS.toNanos(2));
                return (T) new LoginRateLimiter.Decision(false, 1);
            }
        }
        SlowJdbc jdbc = new SlowJdbc();
        var limiter = new LoginRateLimiter(jdbc, 1, 60, now::get);
        assertThat(limiter.check("198.51.100.1").allowed()).isFalse();
        assertThat(limiter.check("198.51.100.1").allowed()).isFalse();
        assertThat(jdbc.calls).isEqualTo(2);
    }
}
