package com.tomrick.mygallery.photo.admin.api.dto;

import java.util.List;

public record AdminPhotoErrorResponse(
        String code,
        String message,
        List<AdminPhotoFieldErrorResponse> fieldErrors
) {

    public AdminPhotoErrorResponse {
        fieldErrors = List.copyOf(fieldErrors);
    }
}
