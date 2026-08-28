package com.tomrick.mygallery.photo.admin.application;

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
import java.util.Map;

@Component
public class UploadSignatureRateLimiter {

    private static final int MAX_REQUESTS = 10;
    private static final int MAX_KEYS = 10_000;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final Map<String, RequestWindow> requests = new HashMap<>();
    private final Clock clock;

    public UploadSignatureRateLimiter() {
        this(Clock.systemUTC());
    }

    UploadSignatureRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public String keyFor(String sessionId, String sourceAddress) {
        String session = sessionId == null ? "" : sessionId;
        String source = sourceAddress == null ? "" : sourceAddress;
        return sha256(session + '\u0000' + source);
    }

    public synchronized RateLimitStatus acquire(String key) {
        Instant now = clock.instant();
        removeExpired(now);
        RequestWindow window = requests.get(key);
        if (window == null) {
            makeRoomForNewKey();
            requests.put(key, new RequestWindow(1, now.plus(WINDOW)));
            return RateLimitStatus.allowed();
        }
        if (window.requestCount() >= MAX_REQUESTS) {
            return RateLimitStatus.blocked(retryAfterSeconds(now, window.expiresAt()));
        }

        requests.put(key, new RequestWindow(window.requestCount() + 1, window.expiresAt()));
        return RateLimitStatus.allowed();
    }

    private void removeExpired(Instant now) {
        requests.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
    }

    private void makeRoomForNewKey() {
        if (requests.size() < MAX_KEYS) {
            return;
        }
        requests.entrySet().stream()
                .min(Comparator.comparing(entry -> entry.getValue().expiresAt()))
                .map(Map.Entry::getKey)
                .ifPresent(requests::remove);
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

    private record RequestWindow(int requestCount, Instant expiresAt) {
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
