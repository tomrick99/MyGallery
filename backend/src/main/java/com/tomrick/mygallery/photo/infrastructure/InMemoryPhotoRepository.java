package com.tomrick.mygallery.photo.infrastructure;

import com.tomrick.mygallery.photo.admin.domain.AdminPhotoPage;
import com.tomrick.mygallery.photo.admin.domain.AdminPhotoRepository;
import com.tomrick.mygallery.photo.admin.domain.AdminPhotoUpdate;
import com.tomrick.mygallery.photo.domain.Photo;
import com.tomrick.mygallery.photo.domain.PhotoRepository;
import com.tomrick.mygallery.photo.domain.PhotoVisibility;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Profile("memory")
public class InMemoryPhotoRepository implements PhotoRepository, AdminPhotoRepository {

    private static final String DERIVATIVE_BASE_URL =
            "https://images.example.test/mygallery/development-derivatives/";
    private static final Comparator<Photo> NEWEST_FIRST = Comparator
            .comparing(Photo::takenAt, Comparator.reverseOrder())
            .thenComparing(Photo::id, Comparator.reverseOrder());

    private final List<Photo> photos = new ArrayList<>(List.of(
            photoWithMetadata(
                    "10000000-0000-0000-0000-000000000101",
                    "orange-steel-over-water",
                    "Orange Steel Over Water",
                    LocalDate.of(2026, 8, 14),
                    "Coastal Pier",
                    6000,
                    4000,
                    true,
                    PhotoVisibility.PUBLIC,
                    "Example Camera X",
                    "Example 35mm Lens",
                    new BigDecimal("35.00"),
                    new BigDecimal("2.80"),
                    new BigDecimal("0.004000000"),
                    400,
                    "Late light across the pier."
            ),
            photo(
                    "10000000-0000-0000-0000-000000000102",
                    "blue-hour-crossing",
                    "Blue Hour Crossing",
                    LocalDate.of(2026, 8, 14),
                    "Haihe River",
                    4000,
                    6000,
                    true,
                    PhotoVisibility.PUBLIC
            ),
            photo(
                    "10000000-0000-0000-0000-000000000103",
                    "quiet-courtyard",
                    "Quiet Courtyard",
                    LocalDate.of(2026, 8, 2),
                    null,
                    4000,
                    4000,
                    false,
                    PhotoVisibility.PUBLIC
            ),
            photo(
                    "10000000-0000-0000-0000-000000000104",
                    "tetrapod-smile",
                    "Tetrapod Smile",
                    LocalDate.of(2026, 7, 21),
                    "Breakwater",
                    6000,
                    3375,
                    true,
                    PhotoVisibility.PUBLIC
            ),
            photo(
                    "10000000-0000-0000-0000-000000000105",
                    "rust-geometry",
                    "Rust Geometry II",
                    LocalDate.of(2025, 11, 18),
                    "Coastal Pier",
                    6000,
                    4000,
                    false,
                    PhotoVisibility.PUBLIC
            ),
            photo(
                    "10000000-0000-0000-0000-000000000106",
                    "neon-suspension",
                    "Neon Suspension",
                    LocalDate.of(2025, 11, 3),
                    "Haihe River",
                    4000,
                    6000,
                    false,
                    PhotoVisibility.PUBLIC
            ),
            photo(
                    "10000000-0000-0000-0000-000000000107",
                    "concrete-shore-study",
                    "Concrete Shore Study",
                    LocalDate.of(2025, 3, 22),
                    "Breakwater",
                    6000,
                    3375,
                    false,
                    PhotoVisibility.PUBLIC
            ),
            photo(
                    "10000000-0000-0000-0000-000000000108",
                    "city-veins-at-night",
                    "City Veins at Night",
                    LocalDate.of(2024, 12, 15),
                    "Daguangming Bridge",
                    4000,
                    4000,
                    false,
                    PhotoVisibility.PUBLIC
            ),
            photo(
                    "10000000-0000-0000-0000-000000000901",
                    "private-featured-study",
                    "Private Featured Study",
                    LocalDate.of(2026, 8, 20),
                    "Development Studio",
                    6000,
                    4000,
                    true,
                    PhotoVisibility.PRIVATE
            ),
            photo(
                    "10000000-0000-0000-0000-000000000902",
                    "private-portrait-study",
                    "Private Portrait Study",
                    LocalDate.of(2025, 11, 25),
                    null,
                    4000,
                    6000,
                    false,
                    PhotoVisibility.PRIVATE
            )
    ));

    @Override
    public synchronized List<Photo> findAllPublic() {
        return photos.stream()
                .filter(InMemoryPhotoRepository::isPublic)
                .toList();
    }

    @Override
    public synchronized Optional<Photo> findPublicById(UUID id) {
        return photos.stream()
                .filter(photo -> photo.id().equals(id))
                .filter(InMemoryPhotoRepository::isPublic)
                .findFirst();
    }

    @Override
    public synchronized AdminPhotoPage findPage(int page, int size) {
        List<Photo> sortedPhotos = photos.stream()
                .sorted(NEWEST_FIRST)
                .toList();
        long offset = (long) page * size;
        if (offset >= sortedPhotos.size()) {
            return new AdminPhotoPage(List.of(), sortedPhotos.size());
        }

        int fromIndex = (int) offset;
        int toIndex = Math.min(fromIndex + size, sortedPhotos.size());
        return new AdminPhotoPage(sortedPhotos.subList(fromIndex, toIndex), sortedPhotos.size());
    }

    @Override
    public synchronized Optional<Photo> findById(UUID id) {
        return photos.stream()
                .filter(photo -> photo.id().equals(id))
                .findFirst();
    }

    @Override
    public synchronized Optional<Photo> update(UUID id, AdminPhotoUpdate update) {
        for (int index = 0; index < photos.size(); index++) {
            Photo existing = photos.get(index);
            if (existing.id().equals(id)) {
                Photo updated = updatedPhoto(existing, update);
                photos.set(index, updated);
                return Optional.of(updated);
            }
        }
        return Optional.empty();
    }

    private static boolean isPublic(Photo photo) {
        return photo.visibility() == PhotoVisibility.PUBLIC;
    }

    private static Photo updatedPhoto(Photo existing, AdminPhotoUpdate update) {
        return new Photo(
                existing.id(),
                update.title(),
                update.takenAt(),
                update.location(),
                existing.width(),
                existing.height(),
                update.featured(),
                update.visibility(),
                existing.thumbnailUrl(),
                existing.cardUrl(),
                existing.displayUrl(),
                update.camera(),
                update.lens(),
                update.focalLengthMm(),
                update.aperture(),
                update.shutterSpeedSeconds(),
                update.iso(),
                update.description()
        );
    }

    private static Photo photo(
            String id,
            String assetName,
            String title,
            LocalDate takenAt,
            String location,
            int width,
            int height,
            boolean featured,
            PhotoVisibility visibility
    ) {
        return photoWithMetadata(
                id,
                assetName,
                title,
                takenAt,
                location,
                width,
                height,
                featured,
                visibility,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private static Photo photoWithMetadata(
            String id,
            String assetName,
            String title,
            LocalDate takenAt,
            String location,
            int width,
            int height,
            boolean featured,
            PhotoVisibility visibility,
            String camera,
            String lens,
            BigDecimal focalLength,
            BigDecimal aperture,
            BigDecimal shutterSpeed,
            Integer iso,
            String description
    ) {
        String derivativeBaseUrl = DERIVATIVE_BASE_URL + assetName + "/";

        return new Photo(
                UUID.fromString(id),
                title,
                takenAt,
                location,
                width,
                height,
                featured,
                visibility,
                derivativeBaseUrl + "thumbnail.jpg",
                derivativeBaseUrl + "card.jpg",
                derivativeBaseUrl + "display.jpg",
                camera,
                lens,
                focalLength,
                aperture,
                shutterSpeed,
                iso,
                description
        );
    }
}
