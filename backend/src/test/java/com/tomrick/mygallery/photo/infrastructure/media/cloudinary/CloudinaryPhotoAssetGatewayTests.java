package com.tomrick.mygallery.photo.infrastructure.media.cloudinary;

import com.cloudinary.Api;
import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.cloudinary.api.ApiResponse;
import com.cloudinary.api.RateLimit;
import com.cloudinary.api.exceptions.NotFound;
import com.cloudinary.http5.ApiStrategy;
import com.cloudinary.http5.UploaderStrategy;
import com.tomrick.mygallery.photo.admin.application.AssetDeleteFailedException;
import com.tomrick.mygallery.photo.admin.application.InvalidUploadedAssetException;
import com.tomrick.mygallery.photo.admin.application.MediaProviderUnavailableException;
import com.tomrick.mygallery.photo.admin.domain.PhotoAssetGateway;
import com.tomrick.mygallery.photo.admin.domain.VerifiedPhotoAsset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CloudinaryPhotoAssetGatewayTests {

    private static final String PUBLIC_ID =
            "mygallery/originals/30000000-1000-4000-8000-000000000001";

    private TestApi api;
    private TestUploader uploader;
    private CloudinaryPhotoAssetGateway gateway;

    @BeforeEach
    void setUp() {
        TestCloudinary cloudinary = new TestCloudinary();
        api = new TestApi(cloudinary);
        uploader = new TestUploader(cloudinary);
        cloudinary.api = api;
        cloudinary.uploader = uploader;
        gateway = new CloudinaryPhotoAssetGateway(cloudinary);
    }

    @Test
    void verificationUsesExactPrivateImageLookupAndMapsOnlyTrustedFields() {
        api.response = response(Map.of(
                "public_id", PUBLIC_ID,
                "resource_type", "image",
                "type", "private",
                "format", "JPG",
                "width", 6048,
                "height", "4024",
                "bytes", 21_000_000L
        ));

        VerifiedPhotoAsset asset = gateway.verifyPrivateImage(PUBLIC_ID);

        assertEquals(PUBLIC_ID, asset.publicId());
        assertEquals("image", asset.resourceType());
        assertEquals("private", asset.deliveryType());
        assertEquals("jpg", asset.format());
        assertEquals(6048, asset.width());
        assertEquals(4024, asset.height());
        assertEquals(21_000_000L, asset.bytes());
        assertEquals(PUBLIC_ID, api.requestedPublicId);
        assertEquals(Map.of("resource_type", "image", "type", "private"), api.options);
    }

    @Test
    void verificationSeparatesMissingAssetsFromProviderFailures() {
        api.failure = new NotFound("missing");
        assertThrows(
                InvalidUploadedAssetException.class,
                () -> gateway.verifyPrivateImage(PUBLIC_ID)
        );

        api.failure = new IOException("provider unavailable");
        assertThrows(
                MediaProviderUnavailableException.class,
                () -> gateway.verifyPrivateImage(PUBLIC_ID)
        );
    }

    @Test
    void deletionTargetsExactPrivateImageAndHandlesProviderOutcomes() {
        uploader.result = Map.of("result", "ok");
        assertEquals(
                PhotoAssetGateway.AssetDeletionResult.DELETED,
                gateway.deletePrivateImage(PUBLIC_ID)
        );
        assertEquals(PUBLIC_ID, uploader.requestedPublicId);
        assertEquals(
                Map.of("resource_type", "image", "type", "private", "invalidate", true),
                uploader.options
        );

        uploader.result = Map.of("result", "not found");
        assertEquals(
                PhotoAssetGateway.AssetDeletionResult.ALREADY_MISSING,
                gateway.deletePrivateImage(PUBLIC_ID)
        );

        uploader.result = Map.of("result", "pending");
        assertThrows(AssetDeleteFailedException.class, () -> gateway.deletePrivateImage(PUBLIC_ID));

        uploader.failure = new IOException("provider unavailable");
        assertThrows(
                MediaProviderUnavailableException.class,
                () -> gateway.deletePrivateImage(PUBLIC_ID)
        );
    }

    private static ApiResponse response(Map<String, Object> values) {
        ApiResponseMap response = new ApiResponseMap();
        response.putAll(values);
        return response;
    }

    private static final class TestCloudinary extends Cloudinary {

        private Api api;
        private Uploader uploader;

        @Override
        public Api api() {
            return api;
        }

        @Override
        public Uploader uploader() {
            return uploader;
        }
    }

    private static final class TestApi extends Api {

        private ApiResponse response;
        private Exception failure;
        private String requestedPublicId;
        private Map<?, ?> options;

        private TestApi(Cloudinary cloudinary) {
            super(cloudinary, new ApiStrategy());
        }

        @Override
        public ApiResponse resource(String publicId, Map options) throws Exception {
            requestedPublicId = publicId;
            this.options = Map.copyOf(options);
            if (failure != null) {
                throw failure;
            }
            return response;
        }
    }

    private static final class TestUploader extends Uploader {

        private Map<String, Object> result;
        private IOException failure;
        private String requestedPublicId;
        private Map<?, ?> options;

        private TestUploader(Cloudinary cloudinary) {
            super(cloudinary, new UploaderStrategy());
        }

        @Override
        public Map destroy(String publicId, Map options) throws IOException {
            requestedPublicId = publicId;
            this.options = Map.copyOf(options);
            if (failure != null) {
                throw failure;
            }
            return result;
        }
    }

    @SuppressWarnings("rawtypes")
    private static final class ApiResponseMap extends HashMap implements ApiResponse {

        @Override
        public Map<String, RateLimit> rateLimits() throws ParseException {
            return Map.of();
        }

        @Override
        public RateLimit apiRateLimit() throws ParseException {
            return null;
        }
    }
}
