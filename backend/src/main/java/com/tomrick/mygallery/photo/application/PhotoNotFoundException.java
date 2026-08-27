package com.tomrick.mygallery.photo.application;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public final class PhotoNotFoundException extends RuntimeException {

    public PhotoNotFoundException() {
        super("Photo not found");
    }
}
