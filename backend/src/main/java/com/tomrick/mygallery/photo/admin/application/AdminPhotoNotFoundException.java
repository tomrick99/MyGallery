package com.tomrick.mygallery.photo.admin.application;

public final class AdminPhotoNotFoundException extends RuntimeException {

    public AdminPhotoNotFoundException() {
        super("Photo not found");
    }
}
