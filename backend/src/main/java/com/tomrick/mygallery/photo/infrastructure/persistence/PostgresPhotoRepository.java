package com.tomrick.mygallery.photo.infrastructure.persistence;

import com.tomrick.mygallery.photo.domain.Photo;
import com.tomrick.mygallery.photo.domain.PhotoRepository;
import com.tomrick.mygallery.photo.domain.PhotoVisibility;
import com.tomrick.mygallery.photo.infrastructure.media.PhotoImageUrlResolver;
import com.tomrick.mygallery.photo.infrastructure.media.PhotoImageUrlResolver.PhotoImageUrls;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Profile("postgres")
@Transactional(readOnly = true)
public class PostgresPhotoRepository implements PhotoRepository {

    private final JpaPhotoEntityRepository entityRepository;
    private final PhotoImageUrlResolver imageUrlResolver;

    public PostgresPhotoRepository(
            JpaPhotoEntityRepository entityRepository,
            PhotoImageUrlResolver imageUrlResolver
    ) {
        this.entityRepository = entityRepository;
        this.imageUrlResolver = imageUrlResolver;
    }

    @Override
    public List<Photo> findAllPublic() {
        return entityRepository
                .findAllByVisibilityOrderByTakenAtDescIdDesc(PhotoVisibility.PUBLIC)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<Photo> findPublicById(UUID id) {
        return entityRepository
                .findByIdAndVisibility(id, PhotoVisibility.PUBLIC)
                .map(this::toDomain);
    }

    private Photo toDomain(PhotoEntity entity) {
        PhotoImageUrls urls = imageUrlResolver.resolve(entity.getCloudinaryPublicId());

        return new Photo(
                entity.getId(),
                entity.getTitle(),
                entity.getTakenAt(),
                entity.getLocation(),
                entity.getWidth(),
                entity.getHeight(),
                entity.isFeatured(),
                entity.getVisibility(),
                urls.thumbnailUrl(),
                urls.cardUrl(),
                urls.displayUrl(),
                entity.getCamera(),
                entity.getLens(),
                entity.getFocalLength(),
                entity.getAperture(),
                entity.getShutterSpeed(),
                entity.getIso(),
                entity.getDescription()
        );
    }
}
