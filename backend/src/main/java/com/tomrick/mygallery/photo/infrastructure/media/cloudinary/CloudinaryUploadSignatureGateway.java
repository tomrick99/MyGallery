package com.tomrick.mygallery.photo.infrastructure.media.cloudinary;

import com.cloudinary.Cloudinary;
import com.tomrick.mygallery.photo.admin.application.MediaProviderUnavailableException;
import com.tomrick.mygallery.photo.admin.domain.UploadSignatureGateway;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Profile("cloudinary")
public final class CloudinaryUploadSignatureGateway implements UploadSignatureGateway {

    private final Cloudinary cloudinary;

    public CloudinaryUploadSignatureGateway(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    public String sign(Map<String, Object> parameters) {
        try {
            return cloudinary.apiSignRequest(
                    parameters,
                    cloudinary.config.apiSecret,
                    cloudinary.config.signatureVersion
            );
        } catch (RuntimeException exception) {
            throw new MediaProviderUnavailableException();
        }
    }
}
