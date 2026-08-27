package com.tomrick.mygallery.photo.infrastructure.media;

import com.tomrick.mygallery.photo.infrastructure.media.PhotoImageUrlResolver.PhotoImageUrls;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DevelopmentPhotoImageUrlResolverTests {

    private final DevelopmentPhotoImageUrlResolver resolver =
            new DevelopmentPhotoImageUrlResolver();

    @Test
    void resolvesDeterministicPlaceholderVariantsWithoutExposingTheInternalAssetId() {
        String publicId = "mygallery/private/internal-asset-id";

        PhotoImageUrls first = resolver.resolve(publicId);
        PhotoImageUrls second = resolver.resolve(publicId);

        assertEquals(first, second);
        assertEquals("thumbnail.jpg", lastPathSegment(first.thumbnailUrl()));
        assertEquals("card.jpg", lastPathSegment(first.cardUrl()));
        assertEquals("display.jpg", lastPathSegment(first.displayUrl()));
        assertFalse(first.thumbnailUrl().contains(publicId));
        assertFalse(first.cardUrl().contains(publicId));
        assertFalse(first.displayUrl().contains(publicId));
    }

    @Test
    void rejectsBlankInternalAssetIds() {
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(" "));
    }

    private static String lastPathSegment(String url) {
        return url.substring(url.lastIndexOf('/') + 1);
    }
}
