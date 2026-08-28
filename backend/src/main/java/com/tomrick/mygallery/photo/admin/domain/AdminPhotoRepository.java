package com.tomrick.mygallery.photo.admin.domain;

import com.tomrick.mygallery.photo.domain.Photo;

import java.util.Optional;
import java.util.UUID;

public interface AdminPhotoRepository {

    AdminPhotoPage findPage(int page, int size);

    Optional<Photo> findById(UUID id);

    Optional<Photo> update(UUID id, AdminPhotoUpdate update);
}
