package com.tomrick.mygallery.photo.admin.api;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminUploadControllerTests {

    private static final String ADMIN_USERNAME = "test-owner";
    private static final String ADMIN_PASSWORD = "test-admin-password";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void signatureRequiresAuthenticationAndCsrf() throws Exception {
        CsrfSession anonymous = bootstrapCsrf("192.0.2.61");
        mockMvc.perform(post("/api/v1/admin/uploads/signature")
                        .session(anonymous.session())
                        .header(anonymous.headerName(), anonymous.token())
                        .with(remoteAddress("192.0.2.61"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUploadDeclaration()))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        CsrfSession admin = loginSuccessfully("192.0.2.62");
        mockMvc.perform(post("/api/v1/admin/uploads/signature")
                        .session(admin.session())
                        .with(remoteAddress("192.0.2.62"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUploadDeclaration()))
                .andExpect(status().isForbidden())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.code").value("CSRF_INVALID"));
    }

    @Test
    void signatureReturnsOnlyTightlyScopedBrowserUploadValues() throws Exception {
        CsrfSession admin = refreshCsrf(loginSuccessfully("192.0.2.63").session(), "192.0.2.63");

        MvcResult first = signature(admin, "192.0.2.63", validUploadDeclaration())
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.cloudName").value("development-cloud"))
                .andExpect(jsonPath("$.apiKey").value("development-api-key"))
                .andExpect(jsonPath("$.resourceType").value("image"))
                .andExpect(jsonPath("$.type").value("private"))
                .andExpect(jsonPath("$.publicId").value(
                        org.hamcrest.Matchers.startsWith("mygallery/originals/")
                ))
                .andExpect(jsonPath("$.uploadPreset").value("development-signed-preset"))
                .andExpect(jsonPath("$.overwrite").value(false))
                .andExpect(jsonPath("$.timestamp").isNumber())
                .andExpect(jsonPath("$.signature").isNotEmpty())
                .andExpect(jsonPath("$.expiresAt").isNotEmpty())
                .andExpect(jsonPath("$.apiSecret").doesNotExist())
                .andExpect(jsonPath("$.sessionId").doesNotExist())
                .andExpect(jsonPath("$.csrfToken").doesNotExist())
                .andExpect(jsonPath("$.originalUrl").doesNotExist())
                .andReturn();

        MvcResult second = signature(admin, "192.0.2.63", validUploadDeclaration())
                .andExpect(status().isOk())
                .andReturn();

        String firstPublicId = JsonPath.read(first.getResponse().getContentAsString(), "$.publicId");
        String secondPublicId = JsonPath.read(second.getResponse().getContentAsString(), "$.publicId");
        assertNotEquals(firstPublicId, secondPublicId);
        assertFalse(firstPublicId.contains("DSC_1234"));

        String responseBody = first.getResponse().getContentAsString();
        assertFalse(responseBody.contains(ADMIN_PASSWORD));
        assertFalse(responseBody.contains(admin.token()));
        assertFalse(responseBody.contains("development-only-cloudinary-signing-secret"));
    }

    @Test
    void signatureRejectsInvalidOrOversizedUploadDeclarations() throws Exception {
        CsrfSession admin = refreshCsrf(loginSuccessfully("192.0.2.64").session(), "192.0.2.64");
        List<String> invalidRequests = List.of(
                uploadDeclaration("", "image/jpeg", 100),
                uploadDeclaration("x".repeat(256), "image/jpeg", 100),
                uploadDeclaration("photo.svg", "image/svg+xml", 100),
                uploadDeclaration("clip.mp4", "video/mp4", 100),
                uploadDeclaration("document.pdf", "application/pdf", 100),
                uploadDeclaration("photo.jpg", "image/jpeg", 0),
                uploadDeclaration("photo.jpg", "image/jpeg", -1),
                uploadDeclaration("photo.jpg", "image/jpeg", 52_428_801L)
        );

        for (String request : invalidRequests) {
            signature(admin, "192.0.2.64", request)
                    .andExpect(status().isBadRequest())
                    .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }
    }

    @Test
    void signatureRateLimitIsBoundedAndReturnsRetryAfter() throws Exception {
        CsrfSession admin = refreshCsrf(loginSuccessfully("192.0.2.65").session(), "192.0.2.65");

        for (int request = 0; request < 10; request++) {
            signature(admin, "192.0.2.65", validUploadDeclaration())
                    .andExpect(status().isOk());
        }

        signature(admin, "192.0.2.65", validUploadDeclaration())
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"))
                .andExpect(jsonPath("$.message").value("Too many upload requests"))
                .andExpect(content().string(not(org.hamcrest.Matchers.containsString("session"))));
    }

    private org.springframework.test.web.servlet.ResultActions signature(
            CsrfSession csrf,
            String remoteAddress,
            String body
    ) throws Exception {
        return mockMvc.perform(post("/api/v1/admin/uploads/signature")
                .session(csrf.session())
                .header(csrf.headerName(), csrf.token())
                .with(remoteAddress(remoteAddress))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private CsrfSession loginSuccessfully(String remoteAddress) throws Exception {
        CsrfSession csrf = bootstrapCsrf(remoteAddress);
        mockMvc.perform(post("/api/v1/admin/session")
                        .session(csrf.session())
                        .header(csrf.headerName(), csrf.token())
                        .with(remoteAddress(remoteAddress))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + ADMIN_USERNAME
                                + "\",\"password\":\"" + ADMIN_PASSWORD + "\"}"))
                .andExpect(status().isOk());
        return csrf;
    }

    private CsrfSession bootstrapCsrf(String remoteAddress) throws Exception {
        return csrfSession(mockMvc.perform(get("/api/v1/admin/csrf")
                        .with(remoteAddress(remoteAddress)))
                .andExpect(status().isOk())
                .andReturn());
    }

    private CsrfSession refreshCsrf(MockHttpSession session, String remoteAddress) throws Exception {
        return csrfSession(mockMvc.perform(get("/api/v1/admin/csrf")
                        .session(session)
                        .with(remoteAddress(remoteAddress)))
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

    private static String validUploadDeclaration() {
        return uploadDeclaration("DSC_1234.jpg", "image/jpeg", 18_432_109);
    }

    private static String uploadDeclaration(String fileName, String contentType, long bytes) {
        return """
                {
                  "fileName": "%s",
                  "contentType": "%s",
                  "bytes": %d
                }
                """.formatted(fileName, contentType, bytes);
    }

    private static RequestPostProcessor remoteAddress(String remoteAddress) {
        return request -> {
            request.setRemoteAddr(remoteAddress);
            return request;
        };
    }

    private record CsrfSession(MockHttpSession session, String headerName, String token) {
    }
}
