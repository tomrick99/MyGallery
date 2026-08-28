package com.tomrick.mygallery.photo.infrastructure.media.cloudinary;

import com.cloudinary.Cloudinary;
import com.tomrick.mygallery.photo.infrastructure.media.PhotoImageUrlResolver.PhotoImageUrls;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CloudinaryPhotoImageUrlResolverTests {

    private static final String API_SECRET = "unit-test-api-secret";
    private static final Pattern DELIVERY_SIGNATURE =
            Pattern.compile("/s--[A-Za-z0-9_-]{8,32}--/");

    private final CloudinaryPhotoImageUrlResolver resolver =
            new CloudinaryPhotoImageUrlResolver(new Cloudinary(Map.of(
                    "cloud_name", "mygallery-test",
                    "api_key", "unit-test-api-key",
                    "api_secret", API_SECRET,
                    "secure", true
            )));

    @Test
    void resolvesThreeSignedSecurePrivateFixedVariants() {
        PhotoImageUrls urls = resolver.resolve("mygallery/originals/opaque-photo-id");

        assertVariant(urls.thumbnailUrl(), 480);
        assertVariant(urls.cardUrl(), 1280);
        assertVariant(urls.displayUrl(), 2048);
        assertNotEquals(urls.thumbnailUrl(), urls.cardUrl());
        assertNotEquals(urls.cardUrl(), urls.displayUrl());
    }

    @Test
    void rejectsBlankInternalAssetIds() {
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(" "));
    }

    private static void assertVariant(String url, int maxDimension) {
        assertTrue(url.startsWith("https://"));
        assertTrue(url.contains("/image/private/"));
        assertTrue(DELIVERY_SIGNATURE.matcher(url).find());
        assertTrue(url.contains("c_limit"));
        assertTrue(url.contains("w_" + maxDimension));
        assertTrue(url.contains("h_" + maxDimension));
        assertTrue(url.contains("q_auto"));
        assertTrue(url.contains("f_auto"));
        assertFalse(url.contains("c_fill"));
        assertFalse(url.contains("c_thumb"));
        assertFalse(url.contains("g_face"));
        assertFalse(url.contains(API_SECRET));
    }
}
