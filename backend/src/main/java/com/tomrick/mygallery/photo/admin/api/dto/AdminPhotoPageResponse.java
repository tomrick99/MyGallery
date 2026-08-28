package com.tomrick.mygallery.photo.admin.api.dto;

import java.util.List;

public record AdminPhotoPageResponse(
        List<AdminPhotoResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public AdminPhotoPageResponse {
        items = List.copyOf(items);
    }
}
