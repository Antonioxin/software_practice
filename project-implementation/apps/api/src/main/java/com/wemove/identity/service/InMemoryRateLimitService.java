package com.wemove.identity.service;

import com.wemove.identity.api.ApiException;
import com.wemove.identity.platform.RateLimitPort;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class InMemoryRateLimitService implements RateLimitPort {
    private final Clock clock;
    private final Map<String, Deque<Instant>> events = new ConcurrentHashMap<>();
    private final Map<String, Instant> loginBlockedUntil = new ConcurrentHashMap<>();

    public InMemoryRateLimitService() { this(Clock.systemUTC()); }
    InMemoryRateLimitService(Clock clock) { this.clock = clock; }

    @Override
    public void consume(String subject, Bucket bucket) {
        int limit = bucket == Bucket.REGISTER ? 5 : 20;
        String key = bucket + ":" + subject;
        Instant now = clock.instant();
        Deque<Instant> queue = events.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (queue) {
            prune(queue, now.minus(Duration.ofMinutes(10)));
            if (queue.size() >= limit) throw rateLimited();
            queue.addLast(now);
        }
    }

    @Override
    public void assertLoginAllowed(String normalizedEmail) {
        Instant until = loginBlockedUntil.get(normalizedEmail);
        if (until != null && until.isAfter(clock.instant())) throw rateLimited();
        if (until != null) loginBlockedUntil.remove(normalizedEmail);
    }

    @Override
    public void recordLoginFailure(String normalizedEmail) {
        String key = "LOGIN:" + normalizedEmail;
        Instant now = clock.instant();
        Deque<Instant> queue = events.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (queue) {
            prune(queue, now.minus(Duration.ofMinutes(10)));
            queue.addLast(now);
            if (queue.size() >= 5) loginBlockedUntil.put(normalizedEmail, now.plus(Duration.ofMinutes(10)));
        }
    }

    @Override
    public void clearLoginFailures(String normalizedEmail) {
        events.remove("LOGIN:" + normalizedEmail);
        loginBlockedUntil.remove(normalizedEmail);
    }

    private void prune(Deque<Instant> queue, Instant boundary) {
        while (!queue.isEmpty() && queue.peekFirst().isBefore(boundary)) queue.removeFirst();
    }

    private ApiException rateLimited() {
        return new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", "请求过于频繁，请 10 分钟后重试。");
    }
}
