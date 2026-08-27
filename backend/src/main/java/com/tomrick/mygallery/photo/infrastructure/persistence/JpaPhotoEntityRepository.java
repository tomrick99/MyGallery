package com.tomrick.mygallery.photo.infrastructure.persistence;

import com.tomrick.mygallery.photo.domain.PhotoVisibility;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface JpaPhotoEntityRepository extends JpaRepository<PhotoEntity, UUID> {

    List<PhotoEntity> findAllByVisibilityOrderByTakenAtDescIdDesc(PhotoVisibility visibility);

    Optional<PhotoEntity> findByIdAndVisibility(UUID id, PhotoVisibility visibility);
}
