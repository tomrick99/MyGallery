package com.tomrick.mygallery.auth.security;

import java.net.URI;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class AdminCorsPolicy {

    private final List<String> allowedOrigins;

    public AdminCorsPolicy(String configuredOrigins) {
        String[] configured = configuredOrigins == null
                ? new String[0]
                : configuredOrigins.split(",");
        var origins = Arrays.stream(configured)
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .map(AdminCorsPolicy::parseConfiguredOrigin)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        this.allowedOrigins = List.copyOf(origins);
    }

    public List<String> allowedOrigins() {
        return allowedOrigins;
    }

    public boolean allowsOriginHeader(String origin) {
        return canonicalOrigin(origin, false)
                .filter(allowedOrigins::contains)
                .isPresent();
    }

    public boolean allowsReferer(String referer) {
        return canonicalOrigin(referer, true)
                .filter(allowedOrigins::contains)
                .isPresent();
    }

    public boolean containsOnlyHttpsOrigins() {
        return allowedOrigins.stream().allMatch(origin -> origin.startsWith("https://"));
    }

    public boolean containsLocalhostOrigin() {
        return allowedOrigins.stream()
                .map(URI::create)
                .map(URI::getHost)
                .anyMatch(AdminCorsPolicy::isLoopbackHost);
    }

    private static String parseConfiguredOrigin(String origin) {
        if (origin.contains("*")) {
            throw new IllegalArgumentException("CORS_ALLOWED_ORIGINS must contain exact origins only");
        }
        return canonicalOrigin(origin, false)
                .orElseThrow(() -> new IllegalArgumentException(
                        "CORS_ALLOWED_ORIGINS must contain exact HTTP(S) origins"
                ));
    }

    private static Optional<String> canonicalOrigin(String value, boolean allowPath) {
        if (value == null || value.isBlank() || value.contains("*")) {
            return Optional.empty();
        }

        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }

        String scheme = uri.getScheme();
        boolean validScheme = "http".equalsIgnoreCase(scheme)
                || "https".equalsIgnoreCase(scheme);
        boolean validPath = allowPath
                || uri.getRawPath() == null
                || uri.getRawPath().isEmpty();
        if (!validScheme
                || uri.getHost() == null
                || uri.getUserInfo() != null
                || !validPath
                || (!allowPath && (uri.getRawQuery() != null || uri.getRawFragment() != null))) {
            return Optional.empty();
        }

        String host = uri.getHost().toLowerCase(Locale.ROOT);
        String formattedHost = host.contains(":") ? "[" + host + "]" : host;
        String port = uri.getPort() < 0 ? "" : ":" + uri.getPort();
        return Optional.of(scheme.toLowerCase(Locale.ROOT) + "://" + formattedHost + port);
    }

    private static boolean isLoopbackHost(String host) {
        if (host == null) {
            return false;
        }
        return "localhost".equalsIgnoreCase(host)
                || "127.0.0.1".equals(host)
                || "::1".equals(host);
    }
}
