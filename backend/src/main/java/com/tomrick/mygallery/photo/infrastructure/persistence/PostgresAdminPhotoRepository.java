package com.tomrick.mygallery.photo.infrastructure.persistence;

import com.tomrick.mygallery.photo.admin.domain.AdminPhotoPage;
import com.tomrick.mygallery.photo.admin.domain.AdminPhotoRepository;
import com.tomrick.mygallery.photo.admin.domain.AdminPhotoUpdate;
import com.tomrick.mygallery.photo.domain.Photo;
import com.tomrick.mygallery.photo.infrastructure.media.PhotoImageUrlResolver;
import com.tomrick.mygallery.photo.infrastructure.media.PhotoImageUrlResolver.PhotoImageUrls;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
@Profile("postgres")
@Transactional(readOnly = true)
public class PostgresAdminPhotoRepository implements AdminPhotoRepository {

    private static final Sort NEWEST_FIRST = Sort.by(
            Sort.Order.desc("takenAt"),
            Sort.Order.desc("id")
    );

    private final JpaPhotoEntityRepository entityRepository;
    private final PhotoImageUrlResolver imageUrlResolver;

    public PostgresAdminPhotoRepository(
            JpaPhotoEntityRepository entityRepository,
            PhotoImageUrlResolver imageUrlResolver
    ) {
        this.entityRepository = entityRepository;
        this.imageUrlResolver = imageUrlResolver;
    }

    @Override
    public AdminPhotoPage findPage(int page, int size) {
        var result = entityRepository.findAll(PageRequest.of(page, size, NEWEST_FIRST));
        return new AdminPhotoPage(
                result.getContent().stream().map(this::toDomain).toList(),
                result.getTotalElements()
        );
    }

    @Override
    public Optional<Photo> findById(UUID id) {
        return entityRepository.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional
    public Optional<Photo> update(UUID id, AdminPhotoUpdate update) {
        return entityRepository.findById(id).map(entity -> {
            entity.updateMetadata(
                    update.title(),
                    update.takenAt(),
                    update.location(),
                    update.featured(),
                    update.visibility(),
                    update.camera(),
                    update.lens(),
                    update.focalLengthMm(),
                    update.aperture(),
                    update.shutterSpeedSeconds(),
                    update.iso(),
                    update.description()
            );
            return toDomain(entity);
        });
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
