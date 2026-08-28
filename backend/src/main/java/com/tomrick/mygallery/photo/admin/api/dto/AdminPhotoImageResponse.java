package com.tomrick.mygallery.photo.admin.api.dto;

public record AdminPhotoImageResponse(
        String thumbnailUrl,
        String cardUrl,
        String displayUrl
) {
}
