package com.tomrick.mygallery.photo.infrastructure.media;

public interface PhotoImageUrlResolver {

    PhotoImageUrls resolve(String cloudinaryPublicId);

    record PhotoImageUrls(
            String thumbnailUrl,
            String cardUrl,
            String displayUrl
    ) {
    }
}
