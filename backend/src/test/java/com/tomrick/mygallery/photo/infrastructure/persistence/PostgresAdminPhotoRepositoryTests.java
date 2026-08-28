package com.tomrick.mygallery.photo.infrastructure.persistence;

import com.tomrick.mygallery.photo.admin.domain.AdminPhotoPage;
import com.tomrick.mygallery.photo.admin.domain.AdminPhotoCreate;
import com.tomrick.mygallery.photo.admin.domain.AdminPhotoUpdate;
import com.tomrick.mygallery.photo.admin.domain.DuplicatePhotoAssetException;
import com.tomrick.mygallery.photo.admin.domain.PhotoAssetIdentity;
import com.tomrick.mygallery.photo.domain.Photo;
import com.tomrick.mygallery.photo.domain.PhotoVisibility;
import com.tomrick.mygallery.photo.infrastructure.media.PhotoImageUrlResolver;
import com.tomrick.mygallery.photo.infrastructure.media.PhotoImageUrlResolver.PhotoImageUrls;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataIntegrityViolationException;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostgresAdminPhotoRepositoryTests {

    private static final UUID PHOTO_ID =
            UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final String CLOUDINARY_PUBLIC_ID = "mygallery/private/admin-photo-asset";

    private PostgresAdminPhotoRepository adminPhotoRepository;
    private List<PhotoEntity> findAllResult;
    private long totalElements;
    private Optional<PhotoEntity> findByIdResult;
    private Pageable queriedPageable;
    private UUID queriedPhotoId;
    private String queriedPublicId;
    private boolean existsByPublicId;
    private PhotoEntity savedEntity;
    private PhotoEntity deletedEntity;
    private boolean flushed;
    private RuntimeException saveFailure;

    @BeforeEach
    void setUp() {
        findAllResult = List.of();
        totalElements = 0;
        findByIdResult = Optional.empty();
        queriedPublicId = null;
        existsByPublicId = false;
        savedEntity = null;
        deletedEntity = null;
        flushed = false;
        saveFailure = null;

        JpaPhotoEntityRepository entityRepository = repositoryProxy();
        PhotoImageUrlResolver imageUrlResolver = ignored -> resolvedUrls();
        adminPhotoRepository = new PostgresAdminPhotoRepository(entityRepository, imageUrlResolver);
    }

    @Test
    void findPageUsesBoundedInfrastructurePagingAndDeterministicOrdering() {
        findAllResult = List.of(privatePhotoEntity());
        totalElements = 12;

        AdminPhotoPage result = adminPhotoRepository.findPage(1, 5);

        assertEquals(1, result.items().size());
        assertEquals(PHOTO_ID, result.items().getFirst().id());
        assertEquals(PhotoVisibility.PRIVATE, result.items().getFirst().visibility());
        assertEquals(12, result.totalElements());
        assertEquals(1, queriedPageable.getPageNumber());
        assertEquals(5, queriedPageable.getPageSize());
        assertEquals(Sort.Direction.DESC, queriedPageable.getSort().getOrderFor("takenAt").getDirection());
        assertEquals(Sort.Direction.DESC, queriedPageable.getSort().getOrderFor("id").getDirection());
    }

    @Test
    void findByIdIsUnrestrictedForAdminReads() {
        findByIdResult = Optional.of(privatePhotoEntity());

        Optional<Photo> result = adminPhotoRepository.findById(PHOTO_ID);

        assertTrue(result.isPresent());
        assertEquals(PHOTO_ID, queriedPhotoId);
        assertEquals(PhotoVisibility.PRIVATE, result.orElseThrow().visibility());
    }

    @Test
    void updateChangesOnlyMutableMetadataAndPreservesVerifiedAssetFields() {
        PhotoEntity entity = privatePhotoEntity();
        findByIdResult = Optional.of(entity);
        AdminPhotoUpdate update = new AdminPhotoUpdate(
                "Updated Metadata",
                LocalDate.of(2026, 7, 1),
                "Updated Location",
                false,
                PhotoVisibility.PUBLIC,
                "Updated Camera",
                "Updated Lens",
                new BigDecimal("85.00"),
                new BigDecimal("5.60"),
                new BigDecimal("0.002000000"),
                1600,
                "Updated description."
        );

        Photo result = adminPhotoRepository.update(PHOTO_ID, update).orElseThrow();

        assertEquals(PHOTO_ID, result.id());
        assertEquals("Updated Metadata", result.title());
        assertEquals(LocalDate.of(2026, 7, 1), result.takenAt());
        assertEquals("Updated Location", result.location());
        assertFalse(result.featured());
        assertEquals(PhotoVisibility.PUBLIC, result.visibility());
        assertEquals("Updated Camera", result.camera());
        assertEquals("Updated Lens", result.lens());
        assertEquals(new BigDecimal("85.00"), result.focalLength());
        assertEquals(new BigDecimal("5.60"), result.aperture());
        assertEquals(new BigDecimal("0.002000000"), result.shutterSpeed());
        assertEquals(1600, result.iso());
        assertEquals("Updated description.", result.description());
        assertEquals(6000, result.width());
        assertEquals(4000, result.height());
        assertEquals(CLOUDINARY_PUBLIC_ID, entity.getCloudinaryPublicId());
    }

    @Test
    void updateReturnsEmptyForAnUnknownPhoto() {
        Optional<Photo> result = adminPhotoRepository.update(
                PHOTO_ID,
                new AdminPhotoUpdate(
                        "Missing",
                        LocalDate.of(2026, 1, 1),
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
                )
        );

        assertTrue(result.isEmpty());
        assertEquals(PHOTO_ID, queriedPhotoId);
    }

    @Test
    void createPersistsVerifiedAssetIdentityAndDimensions() {
        UUID createdId = UUID.fromString("30000000-0000-4000-8000-000000000002");
        AdminPhotoCreate create = new AdminPhotoCreate(
                createdId,
                "mygallery/originals/verified-asset",
                7200,
                4800,
                "Verified Persistence Upload",
                LocalDate.of(2026, 8, 20),
                "Tianjin",
                false,
                PhotoVisibility.PRIVATE,
                "Camera",
                "Lens",
                new BigDecimal("50.00"),
                new BigDecimal("4.00"),
                new BigDecimal("0.008000000"),
                800,
                "Verified before persistence."
        );

        Photo result = adminPhotoRepository.create(create);

        assertEquals(createdId, result.id());
        assertEquals(7200, result.width());
        assertEquals(4800, result.height());
        assertEquals(create.cloudinaryPublicId(), savedEntity.getCloudinaryPublicId());
        assertEquals(PhotoVisibility.PRIVATE, savedEntity.getVisibility());
    }

    @Test
    void duplicateDatabaseConstraintIsMappedToStableDomainConflict() {
        saveFailure = new DataIntegrityViolationException("unique constraint");

        assertThrows(
                DuplicatePhotoAssetException.class,
                () -> adminPhotoRepository.create(new AdminPhotoCreate(
                        UUID.randomUUID(),
                        CLOUDINARY_PUBLIC_ID,
                        6000,
                        4000,
                        "Duplicate",
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
                ))
        );
    }

    @Test
    void assetLookupAndDeleteUseTheStoredInternalIdentity() {
        PhotoEntity entity = privatePhotoEntity();
        findByIdResult = Optional.of(entity);

        PhotoAssetIdentity identity = adminPhotoRepository
                .findAssetIdentityByPhotoId(PHOTO_ID)
                .orElseThrow();
        boolean deleted = adminPhotoRepository.deleteById(PHOTO_ID);

        assertEquals(PHOTO_ID, identity.photoId());
        assertEquals(CLOUDINARY_PUBLIC_ID, identity.cloudinaryPublicId());
        assertTrue(deleted);
        assertSame(entity, deletedEntity);
        assertTrue(flushed);
    }

    @Test
    void duplicatePreflightDelegatesToTheDatabaseIdentityCheck() {
        existsByPublicId = true;

        assertTrue(adminPhotoRepository.existsByCloudinaryPublicId(CLOUDINARY_PUBLIC_ID));
        assertEquals(CLOUDINARY_PUBLIC_ID, queriedPublicId);
    }

    private JpaPhotoEntityRepository repositoryProxy() {
        return (JpaPhotoEntityRepository) Proxy.newProxyInstance(
                JpaPhotoEntityRepository.class.getClassLoader(),
                new Class<?>[]{JpaPhotoEntityRepository.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "findAll" -> {
                        queriedPageable = (Pageable) arguments[0];
                        yield new PageImpl<>(findAllResult, queriedPageable, totalElements);
                    }
                    case "findById" -> {
                        queriedPhotoId = (UUID) arguments[0];
                        yield findByIdResult;
                    }
                    case "existsByCloudinaryPublicId" -> {
                        queriedPublicId = (String) arguments[0];
                        yield existsByPublicId;
                    }
                    case "saveAndFlush" -> {
                        if (saveFailure != null) {
                            throw saveFailure;
                        }
                        savedEntity = (PhotoEntity) arguments[0];
                        yield savedEntity;
                    }
                    case "delete" -> {
                        deletedEntity = (PhotoEntity) arguments[0];
                        yield null;
                    }
                    case "flush" -> {
                        flushed = true;
                        yield null;
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private static PhotoEntity privatePhotoEntity() {
        Instant timestamp = Instant.parse("2026-08-28T00:00:00Z");
        return new PhotoEntity(
                PHOTO_ID,
                "Private Persistence Study",
                LocalDate.of(2026, 8, 28),
                "Tianjin",
                CLOUDINARY_PUBLIC_ID,
                6000,
                4000,
                true,
                PhotoVisibility.PRIVATE,
                "Example Camera",
                "Example Lens",
                new BigDecimal("35.00"),
                new BigDecimal("2.80"),
                new BigDecimal("0.004000000"),
                200,
                "Mapped from the admin persistence boundary.",
                timestamp,
                timestamp
        );
    }

    private static PhotoImageUrls resolvedUrls() {
        return new PhotoImageUrls(
                "https://images.example.test/admin/thumbnail.jpg",
                "https://images.example.test/admin/card.jpg",
                "https://images.example.test/admin/display.jpg"
        );
    }
}
