package com.tomrick.mygallery.photo.infrastructure.media.cloudinary;

import com.cloudinary.Cloudinary;
import com.cloudinary.api.ApiResponse;
import com.cloudinary.api.exceptions.NotFound;
import com.cloudinary.utils.ObjectUtils;
import com.tomrick.mygallery.photo.admin.application.AssetDeleteFailedException;
import com.tomrick.mygallery.photo.admin.application.InvalidUploadedAssetException;
import com.tomrick.mygallery.photo.admin.application.MediaProviderUnavailableException;
import com.tomrick.mygallery.photo.admin.domain.PhotoAssetGateway;
import com.tomrick.mygallery.photo.admin.domain.VerifiedPhotoAsset;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

@Component
@Profile("cloudinary")
public final class CloudinaryPhotoAssetGateway implements PhotoAssetGateway {

    private static final Map<String, Object> PRIVATE_IMAGE_LOOKUP = ObjectUtils.asMap(
            "resource_type", "image",
            "type", "private"
    );
    private static final Map<String, Object> PRIVATE_IMAGE_DELETE = ObjectUtils.asMap(
            "resource_type", "image",
            "type", "private",
            "invalidate", true
    );

    private final Cloudinary cloudinary;

    public CloudinaryPhotoAssetGateway(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    public VerifiedPhotoAsset verifyPrivateImage(String publicId) {
        try {
            ApiResponse resource = cloudinary.api().resource(publicId, PRIVATE_IMAGE_LOOKUP);
            return new VerifiedPhotoAsset(
                    stringValue(resource.get("public_id")),
                    stringValue(resource.get("resource_type")),
                    stringValue(resource.get("type")),
                    stringValue(resource.get("format")).toLowerCase(Locale.ROOT),
                    intValue(resource.get("width")),
                    intValue(resource.get("height")),
                    longValue(resource.get("bytes"))
            );
        } catch (NotFound exception) {
            throw new InvalidUploadedAssetException();
        } catch (Exception exception) {
            throw new MediaProviderUnavailableException();
        }
    }

    @Override
    public AssetDeletionResult deletePrivateImage(String publicId) {
        Map<?, ?> result;
        try {
            result = cloudinary.uploader().destroy(publicId, PRIVATE_IMAGE_DELETE);
        } catch (Exception exception) {
            throw new MediaProviderUnavailableException();
        }

        if (result == null) {
            throw new AssetDeleteFailedException();
        }
        String outcome = stringValue(result.get("result"));
        if ("ok".equals(outcome)) {
            return AssetDeletionResult.DELETED;
        }
        if ("not found".equals(outcome) || "not_found".equals(outcome)) {
            return AssetDeletionResult.ALREADY_MISSING;
        }
        throw new AssetDeleteFailedException();
    }

    private static String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }

    private static int intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(stringValue(value));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private static long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(stringValue(value));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }
}
