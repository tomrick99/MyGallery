package com.tomrick.mygallery.photo.api.dto;

import java.time.LocalDate;
import java.util.UUID;

public record PhotoDetailResponse(
        UUID id,
        String title,
        LocalDate takenAt,
        int year,
        int month,
        String location,
        String orientation,
        double aspectRatio,
        boolean featured,
        PhotoImageResponse image,
        String camera,
        String lens,
        String focalLength,
        String aperture,
        String shutterSpeed,
        Integer iso,
        String description
) {
}
