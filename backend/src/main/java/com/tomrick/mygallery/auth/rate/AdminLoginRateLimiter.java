package com.tomrick.mygallery.auth.rate;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;

@Component
public class AdminLoginRateLimiter {

    private static final int MAX_FAILURES = 5;
    private static final int MAX_KEYS = 10_000;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private final Map<String, FailureWindow> failures = new HashMap<>();
    private final Clock clock;

    public AdminLoginRateLimiter() {
        this(Clock.systemUTC());
    }

    AdminLoginRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public String keyFor(String sourceAddress, String username) {
        String normalizedUsername = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        String source = sourceAddress == null ? "" : sourceAddress;
        return sha256(source + '\u0000' + normalizedUsername);
    }

    public synchronized RateLimitStatus status(String key) {
        Instant now = clock.instant();
        removeExpired(now);
        FailureWindow window = failures.get(key);
        if (window == null || window.failedAttempts() < MAX_FAILURES) {
            return RateLimitStatus.allowed();
        }
        return RateLimitStatus.blocked(retryAfterSeconds(now, window.expiresAt()));
    }

    public synchronized RateLimitStatus recordFailure(String key) {
        Instant now = clock.instant();
        removeExpired(now);
        FailureWindow window = failures.get(key);
        if (window == null) {
            makeRoomForNewKey();
            window = new FailureWindow(0, now.plus(WINDOW));
        }

        FailureWindow updated = new FailureWindow(window.failedAttempts() + 1, window.expiresAt());
        failures.put(key, updated);
        if (updated.failedAttempts() >= MAX_FAILURES) {
            return RateLimitStatus.blocked(retryAfterSeconds(now, updated.expiresAt()));
        }
        return RateLimitStatus.allowed();
    }

    public synchronized void reset(String key) {
        failures.remove(key);
    }

    private void removeExpired(Instant now) {
        failures.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
    }

    private void makeRoomForNewKey() {
        if (failures.size() < MAX_KEYS) {
            return;
        }
        failures.entrySet().stream()
                .min(Comparator.comparing(entry -> entry.getValue().expiresAt()))
                .map(Map.Entry::getKey)
                .ifPresent(failures::remove);
    }

    private static long retryAfterSeconds(Instant now, Instant expiresAt) {
        return Math.max(1, Duration.between(now, expiresAt).toSeconds());
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private record FailureWindow(int failedAttempts, Instant expiresAt) {
    }

    public record RateLimitStatus(boolean blocked, long retryAfterSeconds) {

        static RateLimitStatus allowed() {
            return new RateLimitStatus(false, 0);
        }

        static RateLimitStatus blocked(long retryAfterSeconds) {
            return new RateLimitStatus(true, retryAfterSeconds);
        }
    }
}
