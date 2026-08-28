package com.tomrick.mygallery.photo.admin.api.dto;

import java.time.Instant;

public record UploadSignatureResponse(
        String cloudName,
        String apiKey,
        String resourceType,
        String type,
        String publicId,
        String uploadPreset,
        boolean overwrite,
        long timestamp,
        String signature,
        Instant expiresAt
) {
}
