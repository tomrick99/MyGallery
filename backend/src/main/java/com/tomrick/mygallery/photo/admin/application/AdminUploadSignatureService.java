package com.tomrick.mygallery.photo.admin.application;

import com.tomrick.mygallery.photo.admin.api.dto.UploadSignatureRequest;
import com.tomrick.mygallery.photo.admin.api.dto.UploadSignatureResponse;
import com.tomrick.mygallery.photo.admin.domain.UploadSignatureGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AdminUploadSignatureService {

    private static final String RESOURCE_TYPE = "image";
    private static final String DELIVERY_TYPE = "private";
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/heic",
            "image/heif"
    );

    private final AdminUploadProperties properties;
    private final UploadSignatureGateway signatureGateway;
    private final UploadSignatureRateLimiter rateLimiter;
    private final Clock clock;

    @Autowired
    public AdminUploadSignatureService(
            AdminUploadProperties properties,
            UploadSignatureGateway signatureGateway,
            UploadSignatureRateLimiter rateLimiter
    ) {
        this(properties, signatureGateway, rateLimiter, Clock.systemUTC());
    }

    AdminUploadSignatureService(
            AdminUploadProperties properties,
            UploadSignatureGateway signatureGateway,
            UploadSignatureRateLimiter rateLimiter,
            Clock clock
    ) {
        this.properties = properties;
        this.signatureGateway = signatureGateway;
        this.rateLimiter = rateLimiter;
        this.clock = clock;
    }

    public UploadSignatureResponse issue(
            UploadSignatureRequest request,
            String sessionId,
            String sourceAddress
    ) {
        String rateLimitKey = rateLimiter.keyFor(sessionId, sourceAddress);
        UploadSignatureRateLimiter.RateLimitStatus status = rateLimiter.acquire(rateLimitKey);
        if (status.blocked()) {
            throw new UploadRateLimitExceededException(status.retryAfterSeconds());
        }

        validateDeclaration(request);
        if (!properties.hasBrowserConfiguration()) {
            throw new MediaProviderUnavailableException();
        }

        Instant signedAt = clock.instant();
        String publicId = properties.publicIdPrefix() + "/" + UUID.randomUUID();
        Map<String, Object> signedParameters = signedParameters(publicId, signedAt);
        String signature = signatureGateway.sign(signedParameters);

        return new UploadSignatureResponse(
                properties.cloudName(),
                properties.apiKey(),
                RESOURCE_TYPE,
                DELIVERY_TYPE,
                publicId,
                properties.uploadPreset(),
                false,
                signedAt.getEpochSecond(),
                signature,
                signedAt.plusSeconds(properties.signatureTtlSeconds())
        );
    }

    private void validateDeclaration(UploadSignatureRequest request) {
        if (!ALLOWED_CONTENT_TYPES.contains(request.contentType())
                || request.bytes() > properties.maxBytes()) {
            throw new InvalidUploadDeclarationException();
        }
    }

    private Map<String, Object> signedParameters(String publicId, Instant signedAt) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("public_id", publicId);
        parameters.put("type", DELIVERY_TYPE);
        parameters.put("overwrite", false);
        parameters.put("timestamp", signedAt.getEpochSecond());
        parameters.put("upload_preset", properties.uploadPreset());
        return Map.copyOf(parameters);
    }
}
