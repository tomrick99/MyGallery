package com.tomrick.mygallery.photo.admin.api.dto;

public record AdminPhotoFieldErrorResponse(
        String field,
        String code,
        String message
) {
}
