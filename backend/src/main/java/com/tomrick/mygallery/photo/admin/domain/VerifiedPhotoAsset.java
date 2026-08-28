package com.tomrick.mygallery.photo.admin.domain;

public record VerifiedPhotoAsset(
        String publicId,
        String resourceType,
        String deliveryType,
        String format,
        int width,
        int height,
        long bytes
) {
}
