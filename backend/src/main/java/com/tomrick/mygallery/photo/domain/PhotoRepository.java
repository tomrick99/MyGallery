package com.tomrick.mygallery.photo.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PhotoRepository {

    List<Photo> findAllPublic();

    Optional<Photo> findPublicById(UUID id);
}
