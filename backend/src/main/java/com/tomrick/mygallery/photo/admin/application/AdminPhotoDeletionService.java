package com.tomrick.mygallery.photo.admin.application;

import com.tomrick.mygallery.photo.admin.domain.AdminPhotoRepository;
import com.tomrick.mygallery.photo.admin.domain.PhotoAssetGateway;
import com.tomrick.mygallery.photo.admin.domain.PhotoAssetIdentity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AdminPhotoDeletionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminPhotoDeletionService.class);

    private final AdminPhotoRepository adminPhotoRepository;
    private final PhotoAssetGateway photoAssetGateway;

    public AdminPhotoDeletionService(
            AdminPhotoRepository adminPhotoRepository,
            PhotoAssetGateway photoAssetGateway
    ) {
        this.adminPhotoRepository = adminPhotoRepository;
        this.photoAssetGateway = photoAssetGateway;
    }

    public void delete(UUID id) {
        PhotoAssetIdentity identity = adminPhotoRepository.findAssetIdentityByPhotoId(id)
                .orElseThrow(AdminPhotoNotFoundException::new);

        photoAssetGateway.deletePrivateImage(identity.cloudinaryPublicId());
        if (!adminPhotoRepository.deleteById(id)) {
            throw new AdminPhotoNotFoundException();
        }
        LOGGER.info("Admin photo deleted: photoId={}", id);
    }
}
