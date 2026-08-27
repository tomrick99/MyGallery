package com.tomrick.mygallery.photo.api.dto;

import java.util.List;

public record ArchiveMonthResponse(
        int month,
        String label,
        int photoCount,
        List<PhotoSummaryResponse> photos
) {
}
