package com.tomrick.mygallery.photo.infrastructure.media;

import com.tomrick.mygallery.photo.admin.application.AssetDeleteFailedException;
import com.tomrick.mygallery.photo.admin.application.InvalidUploadedAssetException;
import com.tomrick.mygallery.photo.admin.application.MediaProviderUnavailableException;
import com.tomrick.mygallery.photo.admin.domain.PhotoAssetGateway;
import com.tomrick.mygallery.photo.admin.domain.VerifiedPhotoAsset;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@Profile("memory & !cloudinary")
public class InMemoryPhotoAssetGateway implements PhotoAssetGateway {

    private final Map<String, VerifiedPhotoAsset> assets = new HashMap<>();
    private final Set<String> verificationFailures = new HashSet<>();
    private final Set<String> deletionFailures = new HashSet<>();
    private final List<String> deletionAttempts = new ArrayList<>();

    @Override
    public synchronized VerifiedPhotoAsset verifyPrivateImage(String publicId) {
        if (verificationFailures.contains(publicId)) {
            throw new MediaProviderUnavailableException();
        }
        VerifiedPhotoAsset asset = assets.get(publicId);
        if (asset == null) {
            throw new InvalidUploadedAssetException();
        }
        return asset;
    }

    @Override
    public synchronized AssetDeletionResult deletePrivateImage(String publicId) {
        deletionAttempts.add(publicId);
        if (deletionFailures.contains(publicId)) {
            throw new AssetDeleteFailedException();
        }
        return assets.remove(publicId) == null
                ? AssetDeletionResult.ALREADY_MISSING
                : AssetDeletionResult.DELETED;
    }

    public synchronized void register(VerifiedPhotoAsset asset) {
        assets.put(asset.publicId(), asset);
    }

    public synchronized void failVerification(String publicId) {
        verificationFailures.add(publicId);
    }

    public synchronized void failDeletion(String publicId) {
        deletionFailures.add(publicId);
    }

    public synchronized List<String> deletionAttempts() {
        return List.copyOf(deletionAttempts);
    }
}
