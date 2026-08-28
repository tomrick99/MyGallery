package com.tomrick.mygallery.photo.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record Photo(
        UUID id,
        String title,
        LocalDate takenAt,
        String location,
        int width,
        int height,
        boolean featured,
        PhotoVisibility visibility,
        String thumbnailUrl,
        String cardUrl,
        String displayUrl,
        String camera,
        String lens,
        BigDecimal focalLength,
        BigDecimal aperture,
        BigDecimal shutterSpeed,
        Integer iso,
        String description
) {

    public Photo {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(takenAt, "takenAt must not be null");
        Objects.requireNonNull(visibility, "visibility must not be null");
        Objects.requireNonNull(thumbnailUrl, "thumbnailUrl must not be null");
        Objects.requireNonNull(cardUrl, "cardUrl must not be null");
        Objects.requireNonNull(displayUrl, "displayUrl must not be null");

        if (title != null && title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("width and height must be positive");
        }
    }
}
