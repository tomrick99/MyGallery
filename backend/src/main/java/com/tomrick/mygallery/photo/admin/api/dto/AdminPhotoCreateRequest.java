package com.tomrick.mygallery.photo.admin.api.dto;

import com.tomrick.mygallery.photo.domain.PhotoVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AdminPhotoCreateRequest(
        @NotBlank @Size(max = 255) String cloudinaryPublicId,
        @NotBlank @Size(max = 200) String title,
        @NotNull @PastOrPresent LocalDate takenAt,
        @Size(max = 200) String location,
        boolean featured,
        PhotoVisibility visibility,
        @Size(max = 150) String camera,
        @Size(max = 200) String lens,
        @Positive BigDecimal focalLengthMm,
        @Positive BigDecimal aperture,
        @Positive BigDecimal shutterSpeedSeconds,
        @Positive Integer iso,
        @Size(max = 5000) String description
) {

    public AdminPhotoCreateRequest {
        cloudinaryPublicId = trim(cloudinaryPublicId);
        title = trim(title);
        location = normalizeOptional(location);
        visibility = visibility == null ? PhotoVisibility.PRIVATE : visibility;
        camera = normalizeOptional(camera);
        lens = normalizeOptional(lens);
        description = normalizeOptional(description);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String normalizeOptional(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isEmpty() ? null : trimmed;
    }
}
