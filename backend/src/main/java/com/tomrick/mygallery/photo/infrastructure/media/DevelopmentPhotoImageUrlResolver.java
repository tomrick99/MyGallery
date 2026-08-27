package com.tomrick.mygallery.photo.infrastructure.media;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
@Profile("postgres")
final class DevelopmentPhotoImageUrlResolver implements PhotoImageUrlResolver {

    private static final String BASE_URL =
            "https://images.example.test/mygallery/development-derivatives/";

    @Override
    public PhotoImageUrls resolve(String cloudinaryPublicId) {
        if (cloudinaryPublicId == null || cloudinaryPublicId.isBlank()) {
            throw new IllegalArgumentException("cloudinaryPublicId must not be blank");
        }

        String assetKey = opaqueAssetKey(cloudinaryPublicId);
        String assetBaseUrl = BASE_URL + assetKey + "/";

        return new PhotoImageUrls(
                assetBaseUrl + "thumbnail.jpg",
                assetBaseUrl + "card.jpg",
                assetBaseUrl + "display.jpg"
        );
    }

    private static String opaqueAssetKey(String cloudinaryPublicId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(cloudinaryPublicId.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
