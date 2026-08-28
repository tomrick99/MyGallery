package com.tomrick.mygallery.photo.admin.application;

import com.tomrick.mygallery.photo.admin.api.dto.AdminPhotoCreateRequest;
import com.tomrick.mygallery.photo.admin.api.dto.AdminPhotoResponse;
import com.tomrick.mygallery.photo.admin.domain.AdminPhotoCreate;
import com.tomrick.mygallery.photo.admin.domain.AdminPhotoRepository;
import com.tomrick.mygallery.photo.admin.domain.DuplicatePhotoAssetException;
import com.tomrick.mygallery.photo.admin.domain.PhotoAssetGateway;
import com.tomrick.mygallery.photo.admin.domain.VerifiedPhotoAsset;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.UUID;

@Service
public class AdminPhotoCreationService {

    private final AdminPhotoRepository adminPhotoRepository;
    private final PhotoAssetGateway photoAssetGateway;
    private final AdminUploadProperties uploadProperties;

    public AdminPhotoCreationService(
            AdminPhotoRepository adminPhotoRepository,
            PhotoAssetGateway photoAssetGateway,
            AdminUploadProperties uploadProperties
    ) {
        this.adminPhotoRepository = adminPhotoRepository;
        this.photoAssetGateway = photoAssetGateway;
        this.uploadProperties = uploadProperties;
    }

    public AdminPhotoResponse create(AdminPhotoCreateRequest request) {
        validateOwnedPublicId(request.cloudinaryPublicId());
        if (adminPhotoRepository.existsByCloudinaryPublicId(request.cloudinaryPublicId())) {
            throw new DuplicatePhotoAssetException();
        }

        VerifiedPhotoAsset asset = photoAssetGateway.verifyPrivateImage(
                request.cloudinaryPublicId()
        );
        validateAsset(request.cloudinaryPublicId(), asset);

        AdminPhotoCreate create = new AdminPhotoCreate(
                UUID.randomUUID(),
                asset.publicId(),
                asset.width(),
                asset.height(),
                request.title(),
                request.takenAt(),
                request.location(),
                request.featured(),
                request.visibility(),
                request.camera(),
                request.lens(),
                request.focalLengthMm(),
                request.aperture(),
                request.shutterSpeedSeconds(),
                request.iso(),
                request.description()
        );
        return AdminPhotoService.toResponse(adminPhotoRepository.create(create));
    }

    private void validateOwnedPublicId(String publicId) {
        String prefix = uploadProperties.publicIdPrefix() + "/";
        if (!publicId.startsWith(prefix)) {
            throw new InvalidUploadedAssetException();
        }
        String opaqueId = publicId.substring(prefix.length());
        if (opaqueId.contains("/")) {
            throw new InvalidUploadedAssetException();
        }
        try {
            UUID.fromString(opaqueId);
        } catch (IllegalArgumentException exception) {
            throw new InvalidUploadedAssetException();
        }
    }

    private void validateAsset(String requestedPublicId, VerifiedPhotoAsset asset) {
        String format = asset.format() == null
                ? ""
                : asset.format().trim().toLowerCase(Locale.ROOT);
        boolean valid = requestedPublicId.equals(asset.publicId())
                && "image".equals(asset.resourceType())
                && "private".equals(asset.deliveryType())
                && uploadProperties.allowedFormatSet().contains(format)
                && asset.width() > 0
                && asset.height() > 0
                && asset.bytes() > 0
                && asset.bytes() <= uploadProperties.maxBytes();
        if (!valid) {
            throw new InvalidUploadedAssetException();
        }
    }
}
