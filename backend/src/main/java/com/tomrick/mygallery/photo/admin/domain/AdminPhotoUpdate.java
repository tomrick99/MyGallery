package com.tomrick.mygallery.photo.admin.domain;

import com.tomrick.mygallery.photo.domain.PhotoVisibility;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AdminPhotoUpdate(
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
