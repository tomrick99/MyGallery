package com.tomrick.mygallery.photo.infrastructure.media;

import com.tomrick.mygallery.photo.admin.application.MediaProviderUnavailableException;
import com.tomrick.mygallery.photo.admin.domain.PhotoAssetGateway;
import com.tomrick.mygallery.photo.admin.domain.VerifiedPhotoAsset;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!memory & !cloudinary")
final class UnavailablePhotoAssetGateway implements PhotoAssetGateway {

    @Override
    public VerifiedPhotoAsset verifyPrivateImage(String publicId) {
        throw new MediaProviderUnavailableException();
    }

    @Override
    public AssetDeletionResult deletePrivateImage(String publicId) {
        throw new MediaProviderUnavailableException();
    }
}
