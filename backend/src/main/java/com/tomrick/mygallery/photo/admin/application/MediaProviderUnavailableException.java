package com.tomrick.mygallery.photo.admin.application;

public final class MediaProviderUnavailableException extends RuntimeException {

    public MediaProviderUnavailableException() {
        super("Media provider is temporarily unavailable");
    }
}
