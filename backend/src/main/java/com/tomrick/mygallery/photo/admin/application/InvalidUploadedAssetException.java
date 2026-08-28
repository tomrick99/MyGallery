package com.tomrick.mygallery.photo.admin.application;

public final class InvalidUploadedAssetException extends RuntimeException {

    public InvalidUploadedAssetException() {
        super("Uploaded asset could not be verified");
    }
}
