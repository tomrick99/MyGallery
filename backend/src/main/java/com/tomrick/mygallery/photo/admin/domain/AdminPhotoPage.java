package com.tomrick.mygallery.photo.admin.domain;

import com.tomrick.mygallery.photo.domain.Photo;

import java.util.List;

public record AdminPhotoPage(
        List<Photo> items,
        long totalElements
) {

    public AdminPhotoPage {
        items = List.copyOf(items);
    }
}
