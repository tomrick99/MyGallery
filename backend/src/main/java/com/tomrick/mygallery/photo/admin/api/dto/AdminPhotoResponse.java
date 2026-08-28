package com.tomrick.mygallery.photo.admin.api.dto;

import com.tomrick.mygallery.photo.domain.PhotoVisibility;

import java.time.LocalDate;
import java.util.UUID;

public record AdminPhotoResponse(
        UUID id,
        String title,
        LocalDate takenAt,
        int year,
        int month,
        String location,
        String orientation,
        double aspectRatio,
        boolean featured,
        PhotoVisibility visibility,
        int width,
        int height,
        AdminPhotoImageResponse image,
        String camera,
        String lens,
        String focalLength,
        String aperture,
        String shutterSpeed,
        Integer iso,
        String description
) {
}
