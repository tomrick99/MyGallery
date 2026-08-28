package com.tomrick.mygallery.photo.infrastructure.media;

import com.cloudinary.Cloudinary;
import com.tomrick.mygallery.photo.admin.domain.UploadSignatureGateway;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Profile("!cloudinary")
final class DevelopmentUploadSignatureGateway implements UploadSignatureGateway {

    private static final String DEVELOPMENT_SIGNING_SECRET =
            "development-only-cloudinary-signing-secret";

    private final Cloudinary signer = new Cloudinary(Map.of(
            "api_secret", DEVELOPMENT_SIGNING_SECRET
    ));

    @Override
    public String sign(Map<String, Object> parameters) {
        return signer.apiSignRequest(
                parameters,
                DEVELOPMENT_SIGNING_SECRET,
                signer.config.signatureVersion
        );
    }
}
