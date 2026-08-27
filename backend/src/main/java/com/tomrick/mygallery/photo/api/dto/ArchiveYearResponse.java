package com.tomrick.mygallery.photo.api.dto;

import java.util.List;

public record ArchiveYearResponse(
        int year,
        int photoCount,
        List<ArchiveMonthResponse> months
) {
}
