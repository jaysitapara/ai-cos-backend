package com.app.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fixed-window throttle on credential endpoints, keyed by client IP.
 *
 * <p>Complements the per-account lockout, which cannot see an attacker spraying
 * one password across many accounts — that pattern never trips any single
 * account's counter but is exactly what a leaked-credential run looks like.
 *
 * <p>Scope note: this counts within a single JVM. Behind more than one backend
 * instance the effective limit multiplies by the instance count, which still
 * bounds the attack but is not exact. Move the counter to Redis when the
 * deployment scales out horizontally.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CredentialRateLimiter {

    private final AuthProperties authProperties;

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    /**
     * @return {@code true} when the caller is inside its budget; {@code false}
     *         when the request should be rejected
     */
    public boolean tryAcquire(String clientKey) {
        if (clientKey == null || clientKey.isBlank()) {
            return true;
        }

        long nowSeconds = System.nanoTime() / 1_000_000_000L;
        long windowSeconds = authProperties.rateLimitWindowSeconds();

        Window window = windows.compute(clientKey, (key, existing) ->
            existing == null || nowSeconds - existing.startedAtSeconds() >= windowSeconds
                ? new Window(nowSeconds, new AtomicInteger())
                : existing);

        return window.attempts().incrementAndGet() <= authProperties.rateLimitMaxAttempts();
    }

    /** Drops the counter for a caller that just authenticated successfully. */
    public void reset(String clientKey) {
        if (clientKey != null && !clientKey.isBlank()) {
            windows.remove(clientKey);
        }
    }

    /** Evicts stale windows so the map cannot grow with every distinct client IP. */
    @Scheduled(fixedDelayString = "PT5M")
    void evictExpiredWindows() {
        long nowSeconds = System.nanoTime() / 1_000_000_000L;
        long windowSeconds = authProperties.rateLimitWindowSeconds();
        int removed = 0;

        for (Map.Entry<String, Window> entry : windows.entrySet()) {
            if (nowSeconds - entry.getValue().startedAtSeconds() >= windowSeconds
                && windows.remove(entry.getKey(), entry.getValue())) {
                removed++;
            }
        }

        if (removed > 0) {
            log.debug("Evicted {} expired rate-limit windows", removed);
        }
    }

    private record Window(long startedAtSeconds, AtomicInteger attempts) {
    }
}
