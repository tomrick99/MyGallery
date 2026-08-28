package com.tomrick.mygallery.photo.admin.api;

import com.jayway.jsonpath.JsonPath;
import com.tomrick.mygallery.auth.security.AdminSessionLifetimeFilter;
import com.tomrick.mygallery.photo.admin.domain.AdminPhotoRepository;
import com.tomrick.mygallery.photo.admin.domain.PhotoAssetGateway;
import com.tomrick.mygallery.photo.admin.domain.VerifiedPhotoAsset;
import com.tomrick.mygallery.photo.infrastructure.media.InMemoryPhotoAssetGateway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AdminPhotoLifecycleIntegrationTests {

    private static final String PREFIX = "mygallery/originals/";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PhotoAssetGateway photoAssetGateway;

    @Autowired
    private AdminPhotoRepository adminPhotoRepository;

    @Test
    void verifiedCreateAcceptsHeifProviderMetadataAndRespectsVisibilityBoundaries()
            throws Exception {
        CsrfSession admin = adminCsrf();
        InMemoryPhotoAssetGateway assets = assets();
        String privateAsset = publicId("10000000-1000-4000-8000-000000000001");
        assets.register(validAsset(privateAsset, 7200, 4800, "heif"));

        MvcResult privateCreate = mockMvc.perform(post("/api/v1/admin/photos")
                        .session(admin.session())
                        .header(admin.headerName(), admin.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson(privateAsset, null, true, true)))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.width").value(7200))
                .andExpect(jsonPath("$.height").value(4800))
                .andExpect(jsonPath("$.title").value("Verified Upload"))
                .andExpect(jsonPath("$.visibility").value("PRIVATE"))
                .andExpect(jsonPath("$.cloudinaryPublicId").doesNotExist())
                .andExpect(jsonPath("$.originalUrl").doesNotExist())
                .andExpect(jsonPath("$.createdAt").doesNotExist())
                .andReturn();
        String privateId = JsonPath.read(
                privateCreate.getResponse().getContentAsString(),
                "$.id"
        );
        assertFalse(privateId.equals("ffffffff-ffff-ffff-ffff-ffffffffffff"));
        assertEquals(
                "/api/v1/admin/photos/" + privateId,
                privateCreate.getResponse().getHeader(HttpHeaders.LOCATION)
        );

        mockMvc.perform(get("/api/v1/admin/photos/{id}", privateId).session(admin.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(privateId));
        mockMvc.perform(get("/api/v1/photos/{id}", privateId))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/photos/featured"))
                .andExpect(jsonPath("$[*].id", not(hasItem(privateId))));

        String publicAsset = publicId("10000000-1000-4000-8000-000000000002");
        assets.register(validAsset(publicAsset, 4000, 6000));
        MvcResult publicCreate = create(admin, publicAsset, "PUBLIC", false)
                .andExpect(status().isCreated())
                .andReturn();
        String publicId = JsonPath.read(publicCreate.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/v1/photos/{id}", publicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(publicId))
                .andExpect(jsonPath("$.visibility").doesNotExist())
                .andExpect(jsonPath("$.cloudinaryPublicId").doesNotExist())
                .andExpect(jsonPath("$.width").doesNotExist())
                .andExpect(jsonPath("$.height").doesNotExist())
                .andExpect(jsonPath("$.originalUrl").doesNotExist());
    }

    @Test
    void createAcceptsNullAndBlankTitlesAndReturnsExplicitNull() throws Exception {
        CsrfSession admin = adminCsrf();
        InMemoryPhotoAssetGateway assets = assets();

        String nullTitleAsset = publicId("10000000-1000-4000-8000-000000000009");
        assets.register(validAsset(nullTitleAsset, 6000, 4000));
        MvcResult nullTitleCreate = mockMvc.perform(post("/api/v1/admin/photos")
                        .session(admin.session())
                        .header(admin.headerName(), admin.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson(nullTitleAsset, "PUBLIC", false, false)
                                .replace("\"title\": \"Verified Upload\"", "\"title\": null")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value(org.hamcrest.Matchers.nullValue()))
                .andReturn();
        String nullTitleId = JsonPath.read(
                nullTitleCreate.getResponse().getContentAsString(),
                "$.id"
        );

        mockMvc.perform(get("/api/v1/photos/{id}", nullTitleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(org.hamcrest.Matchers.nullValue()));
        MvcResult publicList = mockMvc.perform(get("/api/v1/photos"))
                .andExpect(status().isOk())
                .andReturn();
        List<Object> matchingTitles = JsonPath.read(
                publicList.getResponse().getContentAsString(),
                "$[?(@.id == '" + nullTitleId + "')].title"
        );
        assertEquals(1, matchingTitles.size());
        assertNull(matchingTitles.getFirst());

        String blankTitleAsset = publicId("10000000-1000-4000-8000-000000000010");
        assets.register(validAsset(blankTitleAsset, 4000, 6000));
        MvcResult blankTitleCreate = mockMvc.perform(post("/api/v1/admin/photos")
                        .session(admin.session())
                        .header(admin.headerName(), admin.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson(blankTitleAsset, "PRIVATE", false, false)
                                .replace("Verified Upload", "   ")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value(org.hamcrest.Matchers.nullValue()))
                .andReturn();
        String blankTitleId = JsonPath.read(
                blankTitleCreate.getResponse().getContentAsString(),
                "$.id"
        );

        mockMvc.perform(get("/api/v1/admin/photos/{id}", blankTitleId)
                        .session(admin.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void createStillRejectsOverlongTitlesAndMissingTakenAt() throws Exception {
        CsrfSession admin = adminCsrf();
        String cloudinaryPublicId = publicId("10000000-1000-4000-8000-000000000011");
        String valid = createJson(cloudinaryPublicId, "PRIVATE", false, false);

        mockMvc.perform(post("/api/v1/admin/photos")
                        .session(admin.session())
                        .header(admin.headerName(), admin.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(valid.replace("Verified Upload", "x".repeat(201))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        mockMvc.perform(post("/api/v1/admin/photos")
                        .session(admin.session())
                        .header(admin.headerName(), admin.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(valid.replace("  \"takenAt\": \"2026-08-20\",\n", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        assertFalse(adminPhotoRepository.existsByCloudinaryPublicId(cloudinaryPublicId));
    }

    @Test
    void duplicateAssetReturnsConflictWithoutDeletingTheLinkedAsset() throws Exception {
        CsrfSession admin = adminCsrf();
        InMemoryPhotoAssetGateway assets = assets();
        String cloudinaryPublicId = publicId("10000000-1000-4000-8000-000000000003");
        assets.register(validAsset(cloudinaryPublicId, 6000, 4000));
        int deleteAttemptsBefore = assets.deletionAttempts().size();

        create(admin, cloudinaryPublicId, "PRIVATE", false)
                .andExpect(status().isCreated());
        create(admin, cloudinaryPublicId, "PRIVATE", false)
                .andExpect(status().isConflict())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.code").value("PHOTO_ASSET_ALREADY_LINKED"));

        assertTrue(adminPhotoRepository.existsByCloudinaryPublicId(cloudinaryPublicId));
        assertEquals(deleteAttemptsBefore, assets.deletionAttempts().size());
    }

    @Test
    void providerVerificationFailureReturnsSanitizedBadGatewayWithoutCreatingARow()
            throws Exception {
        CsrfSession admin = adminCsrf();
        InMemoryPhotoAssetGateway assets = assets();
        String cloudinaryPublicId = publicId("10000000-1000-4000-8000-000000000004");
        assets.failVerification(cloudinaryPublicId);

        create(admin, cloudinaryPublicId, "PRIVATE", false)
                .andExpect(status().isBadGateway())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.code").value("MEDIA_PROVIDER_UNAVAILABLE"));

        assertFalse(adminPhotoRepository.existsByCloudinaryPublicId(cloudinaryPublicId));
    }

    @Test
    void deleteUsesExactInternalIdentityAndRemovesAdminAndPublicRows() throws Exception {
        CsrfSession admin = adminCsrf();
        InMemoryPhotoAssetGateway assets = assets();
        String cloudinaryPublicId = publicId("10000000-1000-4000-8000-000000000005");
        assets.register(validAsset(cloudinaryPublicId, 6000, 4000));
        String photoId = createdPhotoId(admin, cloudinaryPublicId, "PUBLIC");

        mockMvc.perform(delete("/api/v1/admin/photos/{id}", photoId)
                        .session(admin.session())
                        .header(admin.headerName(), admin.token()))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"));

        assertEquals(cloudinaryPublicId, assets.deletionAttempts().getLast());
        mockMvc.perform(get("/api/v1/admin/photos/{id}", photoId).session(admin.session()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/photos/{id}", photoId))
                .andExpect(status().isNotFound());
    }

    @Test
    void providerDeleteFailureLeavesDatabaseRowAndMissingProviderAssetIsRetrySafe()
            throws Exception {
        CsrfSession admin = adminCsrf();
        InMemoryPhotoAssetGateway assets = assets();
        String failingAsset = publicId("10000000-1000-4000-8000-000000000006");
        assets.register(validAsset(failingAsset, 6000, 4000));
        String failingPhotoId = createdPhotoId(admin, failingAsset, "PRIVATE");
        assets.failDeletion(failingAsset);

        mockMvc.perform(delete("/api/v1/admin/photos/{id}", failingPhotoId)
                        .session(admin.session())
                        .header(admin.headerName(), admin.token()))
                .andExpect(status().isBadGateway())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.code").value("ASSET_DELETE_FAILED"));
        mockMvc.perform(get("/api/v1/admin/photos/{id}", failingPhotoId).session(admin.session()))
                .andExpect(status().isOk());

        String missingAsset = publicId("10000000-1000-4000-8000-000000000007");
        assets.register(validAsset(missingAsset, 6000, 4000));
        String missingPhotoId = createdPhotoId(admin, missingAsset, "PRIVATE");
        assertEquals(
                PhotoAssetGateway.AssetDeletionResult.DELETED,
                assets.deletePrivateImage(missingAsset)
        );

        mockMvc.perform(delete("/api/v1/admin/photos/{id}", missingPhotoId)
                        .session(admin.session())
                        .header(admin.headerName(), admin.token()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/admin/photos/{id}", missingPhotoId).session(admin.session()))
                .andExpect(status().isNotFound());
    }

    @Test
    void createAndDeleteRequireAuthenticationAndCsrfAndUnknownDeleteIsNotFound()
            throws Exception {
        String cloudinaryPublicId = publicId("10000000-1000-4000-8000-000000000008");
        CsrfSession anonymous = csrfSession(mockMvc.perform(get("/api/v1/admin/csrf"))
                .andExpect(status().isOk())
                .andReturn());

        mockMvc.perform(post("/api/v1/admin/photos")
                        .session(anonymous.session())
                        .header(anonymous.headerName(), anonymous.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson(cloudinaryPublicId, "PRIVATE", false, false)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/v1/admin/photos/{id}", UUID.randomUUID())
                        .session(anonymous.session())
                        .header(anonymous.headerName(), anonymous.token()))
                .andExpect(status().isUnauthorized());

        MockHttpSession adminWithoutCsrf = adminSession();
        mockMvc.perform(post("/api/v1/admin/photos")
                        .session(adminWithoutCsrf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson(cloudinaryPublicId, "PRIVATE", false, false)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_INVALID"));
        mockMvc.perform(delete("/api/v1/admin/photos/{id}", UUID.randomUUID())
                        .session(adminWithoutCsrf))
                .andExpect(status().isForbidden());

        CsrfSession admin = adminCsrf();
        mockMvc.perform(delete("/api/v1/admin/photos/{id}", UUID.randomUUID())
                        .session(admin.session())
                        .header(admin.headerName(), admin.token()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PHOTO_NOT_FOUND"));
    }

    private org.springframework.test.web.servlet.ResultActions create(
            CsrfSession admin,
            String cloudinaryPublicId,
            String visibility,
            boolean featured
    ) throws Exception {
        return mockMvc.perform(post("/api/v1/admin/photos")
                .session(admin.session())
                .header(admin.headerName(), admin.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson(cloudinaryPublicId, visibility, featured, false)));
    }

    private String createdPhotoId(
            CsrfSession admin,
            String cloudinaryPublicId,
            String visibility
    ) throws Exception {
        MvcResult result = create(admin, cloudinaryPublicId, visibility, false)
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private InMemoryPhotoAssetGateway assets() {
        return assertInstanceOf(InMemoryPhotoAssetGateway.class, photoAssetGateway);
    }

    private static VerifiedPhotoAsset validAsset(String publicId, int width, int height) {
        return validAsset(publicId, width, height, "jpg");
    }

    private static VerifiedPhotoAsset validAsset(
            String publicId,
            int width,
            int height,
            String format
    ) {
        return new VerifiedPhotoAsset(
                publicId,
                "image",
                "private",
                format,
                width,
                height,
                18_432_109L
        );
    }

    private static String publicId(String uuid) {
        return PREFIX + uuid;
    }

    private static String createJson(
            String publicId,
            String visibility,
            boolean featured,
            boolean includeUntrustedTechnicalFields
    ) {
        String visibilityProperty = visibility == null
                ? ""
                : "\n  \"visibility\": \"" + visibility + "\",";
        String untrusted = includeUntrustedTechnicalFields
                ? """

                  "id": "ffffffff-ffff-ffff-ffff-ffffffffffff",
                  "width": 1,
                  "height": 1,
                  "image": {"displayUrl": "https://attacker.invalid/original.jpg"},
                  "createdAt": "2000-01-01T00:00:00Z",
                  "updatedAt": "2000-01-01T00:00:00Z",
                """
                : "";
        return """
                {
                  "cloudinaryPublicId": "%s",
                  "title": "Verified Upload",
                  "takenAt": "2026-08-20",
                  "featured": %s,%s%s
                  "description": "Created only after server-side verification."
                }
                """.formatted(publicId, featured, visibilityProperty, untrusted);
    }

    private CsrfSession adminCsrf() throws Exception {
        MockHttpSession session = adminSession();
        return csrfSession(mockMvc.perform(get("/api/v1/admin/csrf").session(session))
                .andExpect(status().isOk())
                .andReturn());
    }

    private static MockHttpSession adminSession() {
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                "test-owner",
                "not-a-credential",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        ));
        var session = new MockHttpSession();
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                context
        );
        session.setAttribute(
                AdminSessionLifetimeFilter.AUTHENTICATED_AT_ATTRIBUTE,
                Instant.now()
        );
        return session;
    }

    private static CsrfSession csrfSession(MvcResult result) throws Exception {
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        String body = result.getResponse().getContentAsString();
        return new CsrfSession(
                session,
                JsonPath.read(body, "$.headerName"),
                JsonPath.read(body, "$.token")
        );
    }

    private record CsrfSession(MockHttpSession session, String headerName, String token) {
    }
}
