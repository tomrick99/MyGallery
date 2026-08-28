package com.tomrick.mygallery.photo.admin.domain;

import com.tomrick.mygallery.photo.domain.PhotoVisibility;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AdminPhotoCreate(
        UUID id,
        String cloudinaryPublicId,
        int width,
        int height,
        String title,
        LocalDate takenAt,
        String location,
        boolean featured,
        PhotoVisibility visibility,
        String camera,
        String lens,
        BigDecimal focalLengthMm,
        BigDecimal aperture,
        BigDecimal shutterSpeedSeconds,
        Integer iso,
        String description
) {
}
