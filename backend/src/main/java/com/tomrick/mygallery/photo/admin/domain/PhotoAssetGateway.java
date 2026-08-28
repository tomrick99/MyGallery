package com.tomrick.mygallery.photo.admin.domain;

public interface PhotoAssetGateway {

    VerifiedPhotoAsset verifyPrivateImage(String publicId);

    AssetDeletionResult deletePrivateImage(String publicId);

    enum AssetDeletionResult {
        DELETED,
        ALREADY_MISSING
    }
}
