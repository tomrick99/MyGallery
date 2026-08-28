package com.tomrick.mygallery.photo.infrastructure.media.cloudinary;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.tomrick.mygallery.photo.infrastructure.media.PhotoImageUrlResolver;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("postgres & cloudinary")
public final class CloudinaryPhotoImageUrlResolver implements PhotoImageUrlResolver {

    private static final int THUMBNAIL_MAX_DIMENSION = 480;
    private static final int CARD_MAX_DIMENSION = 1280;
    private static final int DISPLAY_MAX_DIMENSION = 2048;

    private final Cloudinary cloudinary;

    public CloudinaryPhotoImageUrlResolver(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    public PhotoImageUrls resolve(String cloudinaryPublicId) {
        if (cloudinaryPublicId == null || cloudinaryPublicId.isBlank()) {
            throw new IllegalArgumentException("cloudinaryPublicId must not be blank");
        }

        return new PhotoImageUrls(
                generateVariant(cloudinaryPublicId, THUMBNAIL_MAX_DIMENSION),
                generateVariant(cloudinaryPublicId, CARD_MAX_DIMENSION),
                generateVariant(cloudinaryPublicId, DISPLAY_MAX_DIMENSION)
        );
    }

    private String generateVariant(String cloudinaryPublicId, int maxDimension) {
        Transformation<?> transformation = new Transformation<>()
                .width(maxDimension)
                .height(maxDimension)
                .crop("limit")
                .quality("auto")
                .fetchFormat("auto");

        return cloudinary.url()
                .resourceType("image")
                .type("private")
                .secure(true)
                .signed(true)
                .transformation(transformation)
                .generate(cloudinaryPublicId);
    }
}
