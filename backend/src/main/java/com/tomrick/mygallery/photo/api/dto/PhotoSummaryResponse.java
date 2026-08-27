package com.tomrick.mygallery.photo.api.dto;

import java.time.LocalDate;
import java.util.UUID;

public record PhotoSummaryResponse(
        UUID id,
        String title,
        LocalDate takenAt,
        int year,
        int month,
        String location,
        String orientation,
        double aspectRatio,
        boolean featured,
        PhotoImageResponse image
) {
}
