package com.tomrick.mygallery.photo.admin.application;

import com.tomrick.mygallery.photo.admin.api.dto.AdminPhotoCreateRequest;
import com.tomrick.mygallery.photo.admin.domain.AdminPhotoCreate;
import com.tomrick.mygallery.photo.admin.domain.AdminPhotoPage;
import com.tomrick.mygallery.photo.admin.domain.AdminPhotoRepository;
import com.tomrick.mygallery.photo.admin.domain.AdminPhotoUpdate;
import com.tomrick.mygallery.photo.admin.domain.PhotoAssetGateway;
import com.tomrick.mygallery.photo.admin.domain.PhotoAssetIdentity;
import com.tomrick.mygallery.photo.admin.domain.VerifiedPhotoAsset;
import com.tomrick.mygallery.photo.domain.Photo;
import com.tomrick.mygallery.photo.domain.PhotoVisibility;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdminPhotoCreationServiceTests {

    private static final String PUBLIC_ID =
            "mygallery/originals/20000000-1000-4000-8000-000000000001";
    private static final long MAX_BYTES = 52_428_800L;
    private static final AdminUploadProperties PROPERTIES = new AdminUploadProperties(
            "test-cloud",
            "test-key",
            "test-preset",
            "mygallery/originals",
            MAX_BYTES,
            120L,
            List.of("jpg", "jpeg", "png", "webp", "heic")
    );

    @Test
    void rejectsEveryUntrustedOrInvalidProviderInvariantBeforePersistence() {
        List<InvalidCase> cases = List.of(
                new InvalidCase(PUBLIC_ID, null),
                new InvalidCase(
                        PUBLIC_ID,
                        asset("mygallery/originals/20000000-1000-4000-8000-000000000099",
                                "image", "private", "jpg", 6000, 4000, 1_000)
                ),
                new InvalidCase(
                        "another-owner/20000000-1000-4000-8000-000000000001",
                        asset(PUBLIC_ID, "image", "private", "jpg", 6000, 4000, 1_000)
                ),
                new InvalidCase(PUBLIC_ID,
                        asset(PUBLIC_ID, "video", "private", "jpg", 6000, 4000, 1_000)),
                new InvalidCase(PUBLIC_ID,
                        asset(PUBLIC_ID, "image", "upload", "jpg", 6000, 4000, 1_000)),
                new InvalidCase(PUBLIC_ID,
                        asset(PUBLIC_ID, "image", "private", "gif", 6000, 4000, 1_000)),
                new InvalidCase(PUBLIC_ID,
                        asset(PUBLIC_ID, "image", "private", "jpg", 0, 4000, 1_000)),
                new InvalidCase(PUBLIC_ID,
                        asset(PUBLIC_ID, "image", "private", "jpg", 6000, 0, 1_000)),
                new InvalidCase(PUBLIC_ID,
                        asset(PUBLIC_ID, "image", "private", "jpg", 6000, 4000, 0)),
                new InvalidCase(PUBLIC_ID,
                        asset(PUBLIC_ID, "image", "private", "jpg", 6000, 4000,
                                MAX_BYTES + 1))
        );

        for (InvalidCase invalidCase : cases) {
            RecordingRepository repository = new RecordingRepository();
            PhotoAssetGateway assetGateway = new FixedAssetGateway(invalidCase.asset());
            var service = new AdminPhotoCreationService(repository, assetGateway, PROPERTIES);

            assertThrows(
                    InvalidUploadedAssetException.class,
                    () -> service.create(request(invalidCase.requestPublicId()))
            );
            assertEquals(0, repository.createCount);
        }
    }

    private static AdminPhotoCreateRequest request(String publicId) {
        return new AdminPhotoCreateRequest(
                publicId,
                "Verified Upload",
                LocalDate.of(2026, 8, 20),
                null,
                false,
                PhotoVisibility.PRIVATE,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private static VerifiedPhotoAsset asset(
            String publicId,
            String resourceType,
            String deliveryType,
            String format,
            int width,
            int height,
            long bytes
    ) {
        return new VerifiedPhotoAsset(
                publicId,
                resourceType,
                deliveryType,
                format,
                width,
                height,
                bytes
        );
    }

    private record InvalidCase(String requestPublicId, VerifiedPhotoAsset asset) {
    }

    private static final class FixedAssetGateway implements PhotoAssetGateway {

        private final VerifiedPhotoAsset asset;

        private FixedAssetGateway(VerifiedPhotoAsset asset) {
            this.asset = asset;
        }

        @Override
        public VerifiedPhotoAsset verifyPrivateImage(String publicId) {
            if (asset == null) {
                throw new InvalidUploadedAssetException();
            }
            return asset;
        }

        @Override
        public AssetDeletionResult deletePrivateImage(String publicId) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class RecordingRepository implements AdminPhotoRepository {

        private int createCount;

        @Override
        public AdminPhotoPage findPage(int page, int size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Photo> findById(UUID id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean existsByCloudinaryPublicId(String cloudinaryPublicId) {
            return false;
        }

        @Override
        public Photo create(AdminPhotoCreate create) {
            createCount++;
            throw new AssertionError("Invalid assets must never reach persistence");
        }

        @Override
        public Optional<Photo> update(UUID id, AdminPhotoUpdate update) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<PhotoAssetIdentity> findAssetIdentityByPhotoId(UUID id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean deleteById(UUID id) {
            throw new UnsupportedOperationException();
        }
    }
}
