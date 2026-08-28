package com.tomrick.mygallery.auth;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminAuthenticationIntegrationTests {

    private static final String ADMIN_USERNAME = "test-owner";
    private static final String ADMIN_PASSWORD = "test-admin-password";
    private static final String TEST_BCRYPT_HASH =
            "$2a$04$vvfatPN0PdqIWH8vfIprVOriHke81ApVotyhpAzHvfLMU4LM1ntz2";
    private static final String PUBLIC_PHOTO_ID = "10000000-0000-0000-0000-000000000101";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicAndHealthGetRoutesRemainAnonymous() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mockMvc.perform(get("/api/v1/photos"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/photos/featured"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/photos/{id}", PUBLIC_PHOTO_ID))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/archive"))
                .andExpect(status().isOk());
    }

    @Test
    void csrfBootstrapCreatesSessionAndReturnsNoStoreToken() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/admin/csrf"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        assertNotNull(result.getRequest().getSession(false));
    }

    @Test
    void loginWithoutCsrfReturnsJsonForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/admin/session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(ADMIN_USERNAME, ADMIN_PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("CSRF_INVALID"));
    }

    @Test
    void invalidLoginPayloadIsSanitizedAndNotCached() throws Exception {
        CsrfSession csrfSession = bootstrapCsrf("192.0.2.9");

        mockMvc.perform(post("/api/v1/admin/session")
                        .session(csrfSession.session())
                        .header(csrfSession.headerName(), csrfSession.token())
                        .with(remoteAddress("192.0.2.9"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"test-owner\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(content().string(not(containsString(ADMIN_USERNAME))));
    }

    @Test
    void invalidUsernameAndPasswordReturnTheSameGenericResponse() throws Exception {
        CsrfSession unknownUserSession = bootstrapCsrf("192.0.2.10");
        String unknownUserResponse = login(
                unknownUserSession,
                "unknown-owner",
                ADMIN_PASSWORD,
                "192.0.2.10"
        )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        CsrfSession wrongPasswordSession = bootstrapCsrf("192.0.2.11");
        String wrongPasswordResponse = login(
                wrongPasswordSession,
                ADMIN_USERNAME,
                "wrong-password",
                "192.0.2.11"
        )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertEquals(unknownUserResponse, wrongPasswordResponse);
        assertFalse(unknownUserResponse.contains("unknown-owner"));
        assertFalse(wrongPasswordResponse.contains("wrong-password"));
    }

    @Test
    void successfulLoginRotatesSessionAndPersistsSecurityContext() throws Exception {
        CsrfSession csrfSession = bootstrapCsrf("192.0.2.20");
        String originalSessionId = csrfSession.session().getId();

        MvcResult loginResult = login(
                csrfSession,
                ADMIN_USERNAME,
                ADMIN_PASSWORD,
                "192.0.2.20"
        )
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.username").value(ADMIN_USERNAME))
                .andExpect(jsonPath("$.authorities").doesNotExist())
                .andReturn();

        assertNotEquals(originalSessionId, csrfSession.session().getId());
        SecurityContext context = (SecurityContext) csrfSession.session().getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY
        );
        assertNotNull(context);
        assertTrue(context.getAuthentication().isAuthenticated());
        assertTrue(context.getAuthentication().getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())));

        mockMvc.perform(get("/api/v1/admin/session")
                        .session(csrfSession.session())
                        .with(remoteAddress("192.0.2.20")))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.username").value(ADMIN_USERNAME));

        String responseBody = loginResult.getResponse().getContentAsString();
        assertFalse(responseBody.contains(ADMIN_PASSWORD));
        assertFalse(responseBody.contains(TEST_BCRYPT_HASH));
        assertFalse(responseBody.contains(originalSessionId));
        assertFalse(responseBody.contains(csrfSession.session().getId()));
        assertFalse(responseBody.contains(csrfSession.token()));
    }

    @Test
    void anonymousSessionLookupReturnsJsonUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/admin/session"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void logoutRequiresCsrf() throws Exception {
        CsrfSession csrfSession = loginSuccessfully("192.0.2.30");

        mockMvc.perform(delete("/api/v1/admin/session")
                        .session(csrfSession.session())
                        .with(remoteAddress("192.0.2.30")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_INVALID"));
    }

    @Test
    void successfulLogoutInvalidatesSessionAndExpiresCookie() throws Exception {
        CsrfSession authenticatedSession = loginSuccessfully("192.0.2.31");
        CsrfSession logoutCsrf = refreshCsrf(authenticatedSession.session(), "192.0.2.31");

        mockMvc.perform(delete("/api/v1/admin/session")
                        .session(logoutCsrf.session())
                        .header(logoutCsrf.headerName(), logoutCsrf.token())
                        .with(remoteAddress("192.0.2.31")))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("mygallery-test-session=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Lax")));

        assertTrue(logoutCsrf.session().isInvalid());
        mockMvc.perform(get("/api/v1/admin/session"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void futureAdminNamespacesAndUnknownRoutesRemainDenied() throws Exception {
        mockMvc.perform(get("/api/v1/admin/photos/future-photo"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
        mockMvc.perform(get("/api/v1/admin/photos"))
                .andExpect(status().isUnauthorized());

        CsrfSession csrfSession = bootstrapCsrf("192.0.2.40");
        mockMvc.perform(post("/api/v1/admin/uploads/signature")
                        .session(csrfSession.session())
                        .header(csrfSession.headerName(), csrfSession.token())
                        .with(remoteAddress("192.0.2.40")))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/unknown"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/photos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void fifthFailedLoginIsRateLimitedWithRetryAfter() throws Exception {
        for (int attempt = 1; attempt < 5; attempt++) {
            CsrfSession csrfSession = bootstrapCsrf("192.0.2.50");
            login(csrfSession, "rate-limited-owner", "wrong-password", "192.0.2.50")
                    .andExpect(status().isUnauthorized());
        }

        CsrfSession fifthAttempt = bootstrapCsrf("192.0.2.50");
        login(fifthAttempt, "rate-limited-owner", "wrong-password", "192.0.2.50")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"))
                .andExpect(jsonPath("$.message").value("Too many login attempts"));

        CsrfSession blockedAttempt = bootstrapCsrf("192.0.2.50");
        login(blockedAttempt, "rate-limited-owner", ADMIN_PASSWORD, "192.0.2.50")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER));
    }

    @Test
    void corsTrustsOnlyTheConfiguredExactOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/admin/session")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type,X-CSRF-TOKEN"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
                .andExpect(header().string(HttpHeaders.VARY, containsString("Origin")));

        mockMvc.perform(options("/api/v1/admin/session")
                        .header(HttpHeaders.ORIGIN, "https://untrusted.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, not("true")));
    }

    private CsrfSession loginSuccessfully(String remoteAddress) throws Exception {
        CsrfSession csrfSession = bootstrapCsrf(remoteAddress);
        login(csrfSession, ADMIN_USERNAME, ADMIN_PASSWORD, remoteAddress)
                .andExpect(status().isOk());
        return csrfSession;
    }

    private CsrfSession bootstrapCsrf(String remoteAddress) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/admin/csrf")
                        .with(remoteAddress(remoteAddress)))
                .andExpect(status().isOk())
                .andReturn();
        return csrfSession(result);
    }

    private CsrfSession refreshCsrf(MockHttpSession session, String remoteAddress) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/admin/csrf")
                        .session(session)
                        .with(remoteAddress(remoteAddress)))
                .andExpect(status().isOk())
                .andReturn();
        return csrfSession(result);
    }

    private CsrfSession csrfSession(MvcResult result) throws Exception {
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        String body = result.getResponse().getContentAsString();
        String headerName = JsonPath.read(body, "$.headerName");
        String token = JsonPath.read(body, "$.token");
        return new CsrfSession(session, headerName, token);
    }

    private ResultActions login(
            CsrfSession csrfSession,
            String username,
            String password,
            String remoteAddress
    ) throws Exception {
        return mockMvc.perform(post("/api/v1/admin/session")
                .session(csrfSession.session())
                .header(csrfSession.headerName(), csrfSession.token())
                .with(remoteAddress(remoteAddress))
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson(username, password)));
    }

    private static String loginJson(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
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
