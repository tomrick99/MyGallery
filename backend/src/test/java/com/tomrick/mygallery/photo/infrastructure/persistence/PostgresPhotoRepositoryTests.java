package com.tomrick.mygallery.photo.infrastructure.persistence;

import com.tomrick.mygallery.photo.domain.Photo;
import com.tomrick.mygallery.photo.domain.PhotoVisibility;
import com.tomrick.mygallery.photo.infrastructure.media.PhotoImageUrlResolver;
import com.tomrick.mygallery.photo.infrastructure.media.PhotoImageUrlResolver.PhotoImageUrls;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostgresPhotoRepositoryTests {

    private static final UUID PHOTO_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final String CLOUDINARY_PUBLIC_ID = "mygallery/private/photo-asset";

    private PostgresPhotoRepository photoRepository;
    private List<PhotoEntity> findAllResult;
    private Optional<PhotoEntity> findByIdResult;
    private PhotoVisibility queriedListVisibility;
    private UUID queriedPhotoId;
    private PhotoVisibility queriedDetailVisibility;
    private String resolvedCloudinaryPublicId;

    @BeforeEach
    void setUp() {
        findAllResult = List.of();
        findByIdResult = Optional.empty();

        JpaPhotoEntityRepository entityRepository = repositoryProxy();
        PhotoImageUrlResolver imageUrlResolver = cloudinaryPublicId -> {
            resolvedCloudinaryPublicId = cloudinaryPublicId;
            return resolvedUrls();
        };
        photoRepository = new PostgresPhotoRepository(entityRepository, imageUrlResolver);
    }

    @Test
    void findAllPublicUsesTheVisibilityBoundQueryAndMapsTheCompleteDomainPhoto() {
        PhotoEntity entity = publicPhotoEntity();
        PhotoImageUrls urls = resolvedUrls();
        findAllResult = List.of(entity);

        List<Photo> photos = photoRepository.findAllPublic();

        assertEquals(1, photos.size());
        Photo photo = photos.getFirst();
        assertEquals(PHOTO_ID, photo.id());
        assertEquals("Persistence Study", photo.title());
        assertEquals(LocalDate.of(2026, 8, 28), photo.takenAt());
        assertEquals("Tianjin", photo.location());
        assertEquals(6000, photo.width());
        assertEquals(4000, photo.height());
        assertTrue(photo.featured());
        assertEquals(PhotoVisibility.PUBLIC, photo.visibility());
        assertEquals(urls.thumbnailUrl(), photo.thumbnailUrl());
        assertEquals(urls.cardUrl(), photo.cardUrl());
        assertEquals(urls.displayUrl(), photo.displayUrl());
        assertEquals("Example Camera", photo.camera());
        assertEquals("Example Lens", photo.lens());
        assertEquals(new BigDecimal("35.00"), photo.focalLength());
        assertEquals(new BigDecimal("2.80"), photo.aperture());
        assertEquals(new BigDecimal("0.004000000"), photo.shutterSpeed());
        assertEquals(200, photo.iso());
        assertEquals("Mapped from the persistence model.", photo.description());
        assertEquals(PhotoVisibility.PUBLIC, queriedListVisibility);
        assertEquals(CLOUDINARY_PUBLIC_ID, resolvedCloudinaryPublicId);
    }

    @Test
    void findPublicByIdUsesIdAndPublicVisibilityAtTheQueryBoundary() {
        Optional<Photo> result = photoRepository.findPublicById(PHOTO_ID);

        assertFalse(result.isPresent());
        assertEquals(PHOTO_ID, queriedPhotoId);
        assertEquals(PhotoVisibility.PUBLIC, queriedDetailVisibility);
    }

    private JpaPhotoEntityRepository repositoryProxy() {
        return (JpaPhotoEntityRepository) Proxy.newProxyInstance(
                JpaPhotoEntityRepository.class.getClassLoader(),
                new Class<?>[]{JpaPhotoEntityRepository.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "findAllByVisibilityOrderByTakenAtDescIdDesc" -> {
                        queriedListVisibility = (PhotoVisibility) arguments[0];
                        yield findAllResult;
                    }
                    case "findByIdAndVisibility" -> {
                        queriedPhotoId = (UUID) arguments[0];
                        queriedDetailVisibility = (PhotoVisibility) arguments[1];
                        yield findByIdResult;
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private static PhotoEntity publicPhotoEntity() {
        Instant timestamp = Instant.parse("2026-08-28T00:00:00Z");
        return new PhotoEntity(
                PHOTO_ID,
                "Persistence Study",
                LocalDate.of(2026, 8, 28),
                "Tianjin",
                CLOUDINARY_PUBLIC_ID,
                6000,
                4000,
                true,
                PhotoVisibility.PUBLIC,
                "Example Camera",
                "Example Lens",
                new BigDecimal("35.00"),
                new BigDecimal("2.80"),
                new BigDecimal("0.004000000"),
                200,
                "Mapped from the persistence model.",
                timestamp,
                timestamp
        );
    }

    private static PhotoImageUrls resolvedUrls() {
        return new PhotoImageUrls(
                "https://images.example.test/opaque/thumbnail.jpg",
                "https://images.example.test/opaque/card.jpg",
                "https://images.example.test/opaque/display.jpg"
        );
    }
}
