package com.tomrick.mygallery.photo.api.dto;

public record PhotoImageResponse(
        String thumbnailUrl,
        String cardUrl,
        String displayUrl
) {
}
