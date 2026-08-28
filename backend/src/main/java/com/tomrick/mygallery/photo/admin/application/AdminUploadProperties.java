package com.tomrick.mygallery.photo.admin.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@ConfigurationProperties(prefix = "mygallery.cloudinary.upload")
public record AdminUploadProperties(
        String cloudName,
        String apiKey,
        String uploadPreset,
        String publicIdPrefix,
        Long maxBytes,
        Long signatureTtlSeconds,
        List<String> allowedFormats
) {

    private static final long DEFAULT_MAX_BYTES = 52_428_800L;
    private static final long DEFAULT_SIGNATURE_TTL_SECONDS = 120L;
    private static final Set<String> DEFAULT_ALLOWED_FORMATS =
            Set.of("jpg", "jpeg", "png", "webp", "heic", "heif");

    public AdminUploadProperties {
        cloudName = trimToEmpty(cloudName);
        apiKey = trimToEmpty(apiKey);
        uploadPreset = trimToEmpty(uploadPreset);
        publicIdPrefix = normalizePrefix(publicIdPrefix);
        maxBytes = maxBytes == null ? DEFAULT_MAX_BYTES : maxBytes;
        signatureTtlSeconds = signatureTtlSeconds == null
                ? DEFAULT_SIGNATURE_TTL_SECONDS
                : signatureTtlSeconds;
        allowedFormats = normalizeFormats(allowedFormats);

        if (maxBytes <= 0) {
            throw new IllegalArgumentException("CLOUDINARY_UPLOAD_MAX_BYTES must be positive");
        }
        if (signatureTtlSeconds < 60 || signatureTtlSeconds > 120) {
            throw new IllegalArgumentException(
                    "CLOUDINARY_UPLOAD_SIGNATURE_TTL_SECONDS must be between 60 and 120"
            );
        }
    }

    public Set<String> allowedFormatSet() {
        return Set.copyOf(allowedFormats);
    }

    public boolean hasBrowserConfiguration() {
        return !cloudName.isBlank() && !apiKey.isBlank() && !uploadPreset.isBlank();
    }

    private static String normalizePrefix(String value) {
        String normalized = trimToEmpty(value);
        if (normalized.isEmpty()) {
            normalized = "mygallery/originals";
        }
        normalized = normalized.replaceAll("^/+|/+$", "");
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(
                    "CLOUDINARY_UPLOAD_PUBLIC_ID_PREFIX must not be blank"
            );
        }
        return normalized;
    }

    private static List<String> normalizeFormats(List<String> values) {
        Set<String> normalized = new LinkedHashSet<>();
        if (values != null) {
            values.stream()
                    .map(AdminUploadProperties::trimToEmpty)
                    .map(value -> value.toLowerCase(Locale.ROOT))
                    .filter(value -> !value.isEmpty())
                    .forEach(normalized::add);
        }
        if (normalized.isEmpty()) {
            normalized.addAll(DEFAULT_ALLOWED_FORMATS);
        }
        return List.copyOf(normalized);
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
