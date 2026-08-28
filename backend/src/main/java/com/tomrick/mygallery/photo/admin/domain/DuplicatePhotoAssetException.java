package com.tomrick.mygallery.photo.admin.domain;

public final class DuplicatePhotoAssetException extends RuntimeException {

    public DuplicatePhotoAssetException() {
        super("Uploaded asset is already linked to a photo");
    }
}
