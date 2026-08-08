package com.cmsBackend.ws.auth.infrastructure.ratelimit;

import com.cmsBackend.ws.auth.application.AuthenticationRateLimiter;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class InMemoryAuthenticationRateLimiter implements AuthenticationRateLimiter {
    private static final int LIMIT = 10;
    private static final Duration WINDOW = Duration.ofMinutes(5);
    private final Map<String, ArrayDeque<Instant>> attempts = new ConcurrentHashMap<>();

    @Override
    public boolean allow(String key) {
        Instant cutoff = Instant.now().minus(WINDOW);
        ArrayDeque<Instant> bucket = attempts.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (bucket) {
            while (!bucket.isEmpty() && bucket.peekFirst().isBefore(cutoff)) bucket.removeFirst();
            if (bucket.size() >= LIMIT) return false;
            bucket.addLast(Instant.now());
            return true;
        }
    }

    @Override
    public void clear(String key) {
        attempts.remove(key);
    }
}
