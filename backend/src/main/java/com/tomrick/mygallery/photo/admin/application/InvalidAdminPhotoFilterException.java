package com.tomrick.mygallery.photo.admin.application;

public final class InvalidAdminPhotoFilterException extends RuntimeException {

    public InvalidAdminPhotoFilterException() {
        super("Invalid admin photo pagination");
    }
}
