package com.tomrick.mygallery.photo.admin.application;

public final class AssetDeleteFailedException extends RuntimeException {

    public AssetDeleteFailedException() {
        super("Asset deletion failed");
    }
}
