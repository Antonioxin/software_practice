package com.wemove.identity.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.wemove.identity.api.ApiException;
import com.wemove.identity.platform.RateLimitPort;
import java.time.*;
import org.junit.jupiter.api.Test;

class InMemoryRateLimitServiceTest {
    @Test
    void blocksLoginForTenMinutesAfterFiveFailures() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-05T00:00:00Z"));
        InMemoryRateLimitService service = new InMemoryRateLimitService(clock);

        for (int i = 0; i < 4; i++) service.recordLoginFailure("adult@example.test");
        assertThatCode(() -> service.assertLoginAllowed("adult@example.test")).doesNotThrowAnyException();

        service.recordLoginFailure("adult@example.test");
        assertThatThrownBy(() -> service.assertLoginAllowed("adult@example.test"))
            .isInstanceOf(ApiException.class)
            .extracting(error -> ((ApiException) error).getCode()).isEqualTo("RATE_LIMITED");

        clock.advance(Duration.ofMinutes(10));
        assertThatCode(() -> service.assertLoginAllowed("adult@example.test")).doesNotThrowAnyException();
    }

    @Test
    void limitsRegistrationBySourceWithinWindow() {
        InMemoryRateLimitService service = new InMemoryRateLimitService(
            new MutableClock(Instant.parse("2026-09-05T00:00:00Z")));

        for (int i = 0; i < 5; i++) service.consume("127.0.0.1", RateLimitPort.Bucket.REGISTER);

        assertThatThrownBy(() -> service.consume("127.0.0.1", RateLimitPort.Bucket.REGISTER))
            .isInstanceOf(ApiException.class)
            .extracting(error -> ((ApiException) error).getStatus().value()).isEqualTo(429);
    }

    private static final class MutableClock extends Clock {
        private Instant instant;
        private MutableClock(Instant instant) { this.instant = instant; }
        void advance(Duration duration) { instant = instant.plus(duration); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return instant; }
    }
}
