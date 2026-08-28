package com.tomrick.mygallery.photo.admin.api;

import com.jayway.jsonpath.JsonPath;
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

import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminPhotoControllerTests {

    private static final String ADMIN_USERNAME = "test-owner";
    private static final String ADMIN_PASSWORD = "test-admin-password";
    private static final String PUBLIC_PHOTO_ID = "10000000-0000-0000-0000-000000000101";
    private static final String PRIVATE_PHOTO_ID = "10000000-0000-0000-0000-000000000901";
    private static final String UNKNOWN_PHOTO_ID = "ffffffff-ffff-ffff-ffff-ffffffffffff";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void adminPhotoRoutesRequireAnAdminRole() throws Exception {
        mockMvc.perform(get("/api/v1/admin/photos"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        var userContext = SecurityContextHolder.createEmptyContext();
        userContext.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                "non-admin",
                "not-a-credential",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        ));
        var userSession = new MockHttpSession();
        userSession.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                userContext
        );

        mockMvc.perform(get("/api/v1/admin/photos").session(userSession))
                .andExpect(status().isForbidden())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void adminListIncludesPublicAndPrivatePhotosInADeterministicPage() throws Exception {
        CsrfSession admin = loginSuccessfully();

        mockMvc.perform(get("/api/v1/admin/photos").session(admin.session()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.items", hasSize(10)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(24))
                .andExpect(jsonPath("$.totalElements").value(10))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.items[0].id").value(PRIVATE_PHOTO_ID))
                .andExpect(jsonPath("$.items[1].id").value("10000000-0000-0000-0000-000000000102"))
                .andExpect(jsonPath("$.items[2].id").value(PUBLIC_PHOTO_ID))
                .andExpect(jsonPath("$.items[*].visibility", hasItems("PUBLIC", "PRIVATE")))
                .andExpect(jsonPath("$.items[0].width").value(6000))
                .andExpect(jsonPath("$.items[0].height").value(4000))
                .andExpect(jsonPath("$.items[0].orientation").value("landscape"))
                .andExpect(jsonPath("$.items[0].aspectRatio").value(1.5))
                .andExpect(jsonPath("$.items[0].image.thumbnailUrl").isString())
                .andExpect(jsonPath("$.items[0].image.cardUrl").isString())
                .andExpect(jsonPath("$.items[0].image.displayUrl").isString())
                .andExpect(jsonPath("$.items[0].cloudinaryPublicId").doesNotExist())
                .andExpect(jsonPath("$.items[0].originalUrl").doesNotExist())
                .andExpect(jsonPath("$.items[0].createdAt").doesNotExist())
                .andExpect(jsonPath("$.items[0].updatedAt").doesNotExist());
    }

    @Test
    void adminListUsesBoundedPaginationAndDoesNotExposeArbitrarySorting() throws Exception {
        CsrfSession admin = loginSuccessfully();

        mockMvc.perform(get("/api/v1/admin/photos")
                        .session(admin.session())
                        .param("page", "1")
                        .param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.items", hasSize(3)))
                .andExpect(jsonPath("$.items[0].id").value("10000000-0000-0000-0000-000000000103"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(3))
                .andExpect(jsonPath("$.totalElements").value(10))
                .andExpect(jsonPath("$.totalPages").value(4));

        mockMvc.perform(get("/api/v1/admin/photos")
                        .session(admin.session())
                        .param("sort", "title,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(PRIVATE_PHOTO_ID));

        for (String path : List.of(
                "/api/v1/admin/photos?page=-1",
                "/api/v1/admin/photos?size=0",
                "/api/v1/admin/photos?size=101",
                "/api/v1/admin/photos?page=not-a-number"
        )) {
            mockMvc.perform(get(path).session(admin.session()))
                    .andExpect(status().isBadRequest())
                    .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                    .andExpect(jsonPath("$.code").value("INVALID_FILTER"));
        }
    }

    @Test
    void adminDetailCanInspectPrivateMetadataWithoutLeakingAssetIdentity() throws Exception {
        CsrfSession admin = loginSuccessfully();

        mockMvc.perform(get("/api/v1/admin/photos/{id}", PRIVATE_PHOTO_ID)
                        .session(admin.session()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.id").value(PRIVATE_PHOTO_ID))
                .andExpect(jsonPath("$.visibility").value("PRIVATE"))
                .andExpect(jsonPath("$.featured").value(true))
                .andExpect(jsonPath("$.cloudinaryPublicId").doesNotExist())
                .andExpect(jsonPath("$.originalUrl").doesNotExist());

        mockMvc.perform(get("/api/v1/admin/photos/{id}", UNKNOWN_PHOTO_ID)
                        .session(admin.session()))
                .andExpect(status().isNotFound())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.code").value("PHOTO_NOT_FOUND"));

        mockMvc.perform(get("/api/v1/admin/photos/not-a-uuid")
                        .session(admin.session()))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(content().string(not(org.hamcrest.Matchers.containsString("UUID"))));
    }

    @Test
    void updateRequiresCsrfAndDoesNotMutateOnFailure() throws Exception {
        CsrfSession admin = loginSuccessfully();

        mockMvc.perform(put("/api/v1/admin/photos/{id}", PUBLIC_PHOTO_ID)
                        .session(admin.session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateJson("PRIVATE")))
                .andExpect(status().isForbidden())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.code").value("CSRF_INVALID"));

        mockMvc.perform(get("/api/v1/admin/photos/{id}", PUBLIC_PHOTO_ID)
                        .session(admin.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Orange Steel Over Water"))
                .andExpect(jsonPath("$.visibility").value("PUBLIC"));
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void updateReplacesMutableMetadataAndCannotChangeImmutableFields() throws Exception {
        CsrfSession admin = loginSuccessfully();
        CsrfSession csrf = refreshCsrf(admin.session());
        String request = validUpdateJson("PRIVATE")
                .replace("\"Managed Title\"", "\"  Curated Frame  \"")
                .replace("\"Managed Location\"", "\"   \"")
                .replace("\"Managed Camera\"", "\"  Camera Z  \"")
                .replace("\"Managed Lens\"", "\"   \"")
                .replace("\"Managed description.\"", "\"  Refined description.  \"")
                .replace(
                        "\n}",
                        ",\n  \"id\": \"ffffffff-ffff-ffff-ffff-ffffffffffff\",\n"
                                + "  \"width\": 1,\n"
                                + "  \"height\": 1,\n"
                                + "  \"cloudinaryPublicId\": \"attacker-controlled\"\n}"
                );

        mockMvc.perform(put("/api/v1/admin/photos/{id}", PUBLIC_PHOTO_ID)
                        .session(csrf.session())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.id").value(PUBLIC_PHOTO_ID))
                .andExpect(jsonPath("$.title").value("Curated Frame"))
                .andExpect(jsonPath("$.takenAt").value("2026-08-01"))
                .andExpect(jsonPath("$.location").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.featured").value(true))
                .andExpect(jsonPath("$.visibility").value("PRIVATE"))
                .andExpect(jsonPath("$.width").value(6000))
                .andExpect(jsonPath("$.height").value(4000))
                .andExpect(jsonPath("$.camera").value("Camera Z"))
                .andExpect(jsonPath("$.lens").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.focalLength").value("50 mm"))
                .andExpect(jsonPath("$.aperture").value("f/4"))
                .andExpect(jsonPath("$.shutterSpeed").value("1/125 s"))
                .andExpect(jsonPath("$.iso").value(800))
                .andExpect(jsonPath("$.description").value("Refined description."))
                .andExpect(jsonPath("$.cloudinaryPublicId").doesNotExist());
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void invalidUpdatesReturnFieldErrorsAndNeverPartiallyMutate() throws Exception {
        CsrfSession admin = loginSuccessfully();
        CsrfSession csrf = refreshCsrf(admin.session());
        String valid = validUpdateJson("PRIVATE");
        List<String> invalidRequests = List.of(
                valid.replace("\"Managed Title\"", "\"   \""),
                valid.replace("\"Managed Title\"", "\"" + "x".repeat(201) + "\""),
                valid.replace("\"takenAt\": \"2026-08-01\",", ""),
                valid.replace("2026-08-01", "2999-01-01"),
                valid.replace("  \"featured\": true,\n", ""),
                valid.replace("  \"visibility\": \"PRIVATE\",\n", ""),
                valid.replace("\"visibility\": \"PRIVATE\"", "\"visibility\": \"DRAFT\""),
                valid.replace("\"Managed Location\"", "\"" + "l".repeat(201) + "\""),
                valid.replace("\"Managed Camera\"", "\"" + "c".repeat(151) + "\""),
                valid.replace("\"Managed Lens\"", "\"" + "n".repeat(201) + "\""),
                valid.replace("\"focalLengthMm\": 50", "\"focalLengthMm\": 0"),
                valid.replace("\"aperture\": 4", "\"aperture\": 0"),
                valid.replace("\"shutterSpeedSeconds\": 0.008", "\"shutterSpeedSeconds\": 0"),
                valid.replace("\"iso\": 800", "\"iso\": 0"),
                valid.replace("\"Managed description.\"", "\"" + "d".repeat(5001) + "\"")
        );

        for (String invalidRequest : invalidRequests) {
            mockMvc.perform(put("/api/v1/admin/photos/{id}", PUBLIC_PHOTO_ID)
                            .session(csrf.session())
                            .header(csrf.headerName(), csrf.token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andExpect(status().isBadRequest())
                    .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }

        mockMvc.perform(get("/api/v1/admin/photos/{id}", PUBLIC_PHOTO_ID)
                        .session(csrf.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Orange Steel Over Water"))
                .andExpect(jsonPath("$.visibility").value("PUBLIC"));
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void visibilityChangesImmediatelyRespectEveryPublicBoundary() throws Exception {
        CsrfSession admin = loginSuccessfully();
        CsrfSession csrf = refreshCsrf(admin.session());

        mockMvc.perform(put("/api/v1/admin/photos/{id}", PRIVATE_PHOTO_ID)
                        .session(csrf.session())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateJson("PUBLIC")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visibility").value("PUBLIC"));

        mockMvc.perform(get("/api/v1/photos/{id}", PRIVATE_PHOTO_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visibility").doesNotExist());
        mockMvc.perform(get("/api/v1/photos"))
                .andExpect(jsonPath("$[*].id", hasItem(PRIVATE_PHOTO_ID)));
        mockMvc.perform(get("/api/v1/photos/featured"))
                .andExpect(jsonPath("$[*].id", hasItem(PRIVATE_PHOTO_ID)));
        mockMvc.perform(get("/api/v1/archive"))
                .andExpect(jsonPath("$..id", hasItem(PRIVATE_PHOTO_ID)));

        mockMvc.perform(put("/api/v1/admin/photos/{id}", PRIVATE_PHOTO_ID)
                        .session(csrf.session())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateJson("PRIVATE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.featured").value(true))
                .andExpect(jsonPath("$.visibility").value("PRIVATE"));

        mockMvc.perform(get("/api/v1/photos/{id}", PRIVATE_PHOTO_ID))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/photos"))
                .andExpect(jsonPath("$[*].id", not(hasItem(PRIVATE_PHOTO_ID))));
        mockMvc.perform(get("/api/v1/photos/featured"))
                .andExpect(jsonPath("$[*].id", not(hasItem(PRIVATE_PHOTO_ID))));
        mockMvc.perform(get("/api/v1/archive"))
                .andExpect(jsonPath("$..id", not(hasItem(PRIVATE_PHOTO_ID))));
    }

    @Test
    void unknownPhotoUpdateReturnsSanitizedNoStoreNotFound() throws Exception {
        CsrfSession admin = loginSuccessfully();
        CsrfSession csrf = refreshCsrf(admin.session());

        mockMvc.perform(put("/api/v1/admin/photos/{id}", UNKNOWN_PHOTO_ID)
                        .session(csrf.session())
                        .header(csrf.headerName(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateJson("PUBLIC")))
                .andExpect(status().isNotFound())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.code").value("PHOTO_NOT_FOUND"))
                .andExpect(content().string(not(org.hamcrest.Matchers.containsString("Exception"))));
    }

    private CsrfSession loginSuccessfully() throws Exception {
        CsrfSession bootstrap = csrfSession(mockMvc.perform(get("/api/v1/admin/csrf"))
                .andExpect(status().isOk())
                .andReturn());
        mockMvc.perform(post("/api/v1/admin/session")
                        .session(bootstrap.session())
                        .header(bootstrap.headerName(), bootstrap.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + ADMIN_USERNAME
                                + "\",\"password\":\"" + ADMIN_PASSWORD + "\"}"))
                .andExpect(status().isOk());
        return bootstrap;
    }

    private CsrfSession refreshCsrf(MockHttpSession session) throws Exception {
        return csrfSession(mockMvc.perform(get("/api/v1/admin/csrf").session(session))
                .andExpect(status().isOk())
                .andReturn());
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

    private static String validUpdateJson(String visibility) {
        return """
                {
                  "title": "Managed Title",
                  "takenAt": "2026-08-01",
                  "location": "Managed Location",
                  "featured": true,
                  "visibility": "%s",
                  "camera": "Managed Camera",
                  "lens": "Managed Lens",
                  "focalLengthMm": 50,
                  "aperture": 4,
                  "shutterSpeedSeconds": 0.008,
                  "iso": 800,
                  "description": "Managed description."
                }
                """.formatted(visibility);
    }

    private record CsrfSession(MockHttpSession session, String headerName, String token) {
    }
}
