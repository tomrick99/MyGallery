package com.tomrick.mygallery.photo.admin.application;

import com.tomrick.mygallery.photo.admin.api.dto.UploadSignatureRequest;
import com.tomrick.mygallery.photo.admin.api.dto.UploadSignatureResponse;
import com.tomrick.mygallery.photo.admin.domain.UploadSignatureGateway;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminUploadSignatureServiceTests {

    @Test
    void signsOnlyCloudinaryUploadParametersThatMustBeBound() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-28T06:18:00Z"), ZoneOffset.UTC);
        var rateLimiter = new UploadSignatureRateLimiter(clock);
        var signatureGateway = new CapturingSignatureGateway();
        var properties = new AdminUploadProperties(
                "test-cloud",
                "test-api-key",
                "test-signed-preset",
                "mygallery/originals",
                52_428_800L,
                120L,
                List.of("jpg", "jpeg", "png", "webp", "heic")
        );
        var service = new AdminUploadSignatureService(
                properties,
                signatureGateway,
                rateLimiter,
                clock
        );

        UploadSignatureResponse response = service.issue(
                new UploadSignatureRequest("DSC_1234.jpg", "image/jpeg", 1_000L),
                "opaque-session",
                "192.0.2.70"
        );

        assertEquals(
                java.util.Set.of("public_id", "type", "overwrite", "timestamp", "upload_preset"),
                signatureGateway.parameters().keySet()
        );
        assertEquals(response.publicId(), signatureGateway.parameters().get("public_id"));
        assertEquals("private", signatureGateway.parameters().get("type"));
        assertEquals(false, signatureGateway.parameters().get("overwrite"));
        assertEquals(
                Instant.parse("2026-08-28T06:18:00Z").getEpochSecond(),
                signatureGateway.parameters().get("timestamp")
        );
        assertEquals("test-signed-preset", signatureGateway.parameters().get("upload_preset"));
        assertFalse(signatureGateway.parameters().containsKey("resource_type"));
        assertFalse(signatureGateway.parameters().containsKey("api_key"));
        assertFalse(signatureGateway.parameters().containsKey("cloud_name"));
        assertFalse(signatureGateway.parameters().containsKey("file"));
        assertEquals("image", response.resourceType());
        assertEquals("private", response.type());
        assertFalse(response.overwrite());
        assertEquals(Instant.parse("2026-08-28T06:20:00Z"), response.expiresAt());
        assertTrue(response.publicId().startsWith("mygallery/originals/"));
    }

    private static final class CapturingSignatureGateway implements UploadSignatureGateway {

        private Map<String, Object> parameters;

        @Override
        public String sign(Map<String, Object> parameters) {
            this.parameters = Map.copyOf(parameters);
            return "sdk-signature";
        }

        Map<String, Object> parameters() {
            return parameters;
        }
    }
}
