package com.tomrick.mygallery.auth;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.server.Cookie;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.boot.web.server.servlet.Session;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        properties = {
                "mygallery.auth.cors.allowed-origins=https://www.example.test",
                "mygallery.auth.session.absolute-timeout=8h",
                "server.servlet.session.cookie.name=__Host-mygallery-session",
                "server.servlet.session.cookie.secure=true"
        },
        classes = {
                com.tomrick.mygallery.MyGalleryApplication.class,
                ProductionSecurityIntegrationTests.ClockConfiguration.class
        }
)
@AutoConfigureMockMvc
@ActiveProfiles({"memory", "prod"})
class ProductionSecurityIntegrationTests {

    private static final String ALLOWED_ORIGIN = "https://www.example.test";
    private static final String ADMIN_USERNAME = "test-owner";
    private static final String ADMIN_PASSWORD = "test-admin-password";
    private static final Instant START = Instant.parse("2026-08-28T08:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MutableClock clock;

    @Autowired
    private ServerProperties serverProperties;

    @BeforeEach
    void resetClock() {
        clock.set(START);
    }

    @Test
    void productionCookieHeadersRequestIdAndActuatorExposureAreHardened() throws Exception {
        var session = serverProperties.getServlet().getSession();
        var cookie = session.getCookie();
        assertEquals(Duration.ofMinutes(30), session.getTimeout());
        assertEquals(Set.of(Session.SessionTrackingMode.COOKIE), session.getTrackingModes());
        assertEquals("__Host-mygallery-session", cookie.getName());
        assertTrue(cookie.getSecure());
        assertTrue(cookie.getHttpOnly());
        assertEquals(Cookie.SameSite.LAX, cookie.getSameSite());
        assertEquals("/", cookie.getPath());
        assertNull(cookie.getDomain());
        assertEquals(
                ServerProperties.ForwardHeadersStrategy.FRAMEWORK,
                serverProperties.getForwardHeadersStrategy()
        );

        MvcResult secureResponse = mockMvc.perform(get("/api/v1/photos")
                        .secure(true)
                        .header("X-Request-ID", "client-controlled-request-id"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-ID"))
                .andExpect(header().string("Strict-Transport-Security", "max-age=31536000"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string(
                        "Content-Security-Policy",
                        "default-src 'none'; frame-ancestors 'none'"
                ))
                .andReturn();
        assertNotEquals(
                "client-controlled-request-id",
                secureResponse.getResponse().getHeader("X-Request-ID")
        );

        mockMvc.perform(get("/api/v1/photos"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Strict-Transport-Security"));

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/mappings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void productionAdminMutationsRequireAllowedOriginOrRefererAndStillRequireCsrf()
            throws Exception {
        CsrfSession admin = loginSuccessfully("192.0.2.81");
        CsrfSession csrf = refreshCsrf(admin.session(), "192.0.2.81");

        mockMvc.perform(signature(csrf, "192.0.2.81")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN))
                .andExpect(status().isOk());

        mockMvc.perform(signature(csrf, "192.0.2.81")
                        .header(
                                HttpHeaders.REFERER,
                                ALLOWED_ORIGIN + "/admin/photos?view=upload"
                        ))
                .andExpect(status().isOk());

        mockMvc.perform(signature(csrf, "192.0.2.81")
                        .header(HttpHeaders.ORIGIN, "https://attacker.example"))
                .andExpect(status().isForbidden())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.code").value("ORIGIN_FORBIDDEN"))
                .andExpect(content().string(not(containsString(ALLOWED_ORIGIN))));

        mockMvc.perform(signature(csrf, "192.0.2.81")
                        .header(HttpHeaders.REFERER, "https://attacker.example/admin"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ORIGIN_FORBIDDEN"));

        mockMvc.perform(signature(csrf, "192.0.2.81"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ORIGIN_FORBIDDEN"));

        mockMvc.perform(post("/api/v1/admin/uploads/signature")
                        .session(csrf.session())
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUploadDeclaration()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_INVALID"));

        mockMvc.perform(options("/api/v1/admin/uploads/signature")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(
                                HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
                                "Content-Type,X-CSRF-TOKEN"
                        ))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        ALLOWED_ORIGIN
                ))
                .andExpect(header().string(HttpHeaders.VARY, containsString("Origin")))
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                        containsString("X-Request-ID")
                ));

        mockMvc.perform(get("/api/v1/photos"))
                .andExpect(status().isOk());
    }

    @Test
    void absoluteAdminSessionLifetimeExpiresAndInvalidatesTheServerSession() throws Exception {
        CsrfSession admin = loginSuccessfully("192.0.2.82");

        mockMvc.perform(get("/api/v1/admin/session").session(admin.session()))
                .andExpect(status().isOk());

        clock.set(START.plus(Duration.ofHours(8)).minusSeconds(1));
        mockMvc.perform(get("/api/v1/admin/session").session(admin.session()))
                .andExpect(status().isOk());

        clock.set(START.plus(Duration.ofHours(8)).plusSeconds(1));
        MvcResult expired = mockMvc.perform(get("/api/v1/admin/session")
                        .session(admin.session()))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(header().exists("X-Request-ID"))
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        containsString("__Host-mygallery-session=")
                ))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Secure")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Lax")))
                .andExpect(jsonPath("$.code").value("SESSION_EXPIRED"))
                .andExpect(content().string(not(containsString(START.toString()))))
                .andReturn();

        assertTrue(admin.session().isInvalid());
        assertFalse(expired.getResponse().getContentAsString().contains(admin.originalSessionId()));
        assertFalse(expired.getResponse().getContentAsString().contains(admin.rotatedSessionId()));

        mockMvc.perform(get("/api/v1/admin/session"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    private CsrfSession loginSuccessfully(String remoteAddress) throws Exception {
        CsrfSession csrf = bootstrapCsrf(remoteAddress);
        String originalSessionId = csrf.session().getId();
        mockMvc.perform(post("/api/v1/admin/session")
                        .session(csrf.session())
                        .header(csrf.headerName(), csrf.token())
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                        .with(request -> {
                            request.setRemoteAddr(remoteAddress);
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + ADMIN_USERNAME
                                + "\",\"password\":\"" + ADMIN_PASSWORD + "\"}"))
                .andExpect(status().isOk());
        String rotatedSessionId = csrf.session().getId();
        assertNotEquals(originalSessionId, rotatedSessionId);
        return new CsrfSession(
                csrf.session(),
                csrf.headerName(),
                csrf.token(),
                originalSessionId,
                rotatedSessionId
        );
    }

    private CsrfSession bootstrapCsrf(String remoteAddress) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/admin/csrf")
                        .with(request -> {
                            request.setRemoteAddr(remoteAddress);
                            return request;
                        }))
                .andExpect(status().isOk())
                .andReturn();
        return csrfSession(result, null, null);
    }

    private CsrfSession refreshCsrf(MockHttpSession session, String remoteAddress) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/admin/csrf")
                        .session(session)
                        .with(request -> {
                            request.setRemoteAddr(remoteAddress);
                            return request;
                        }))
                .andExpect(status().isOk())
                .andReturn();
        return csrfSession(result, session.getId(), session.getId());
    }

    private static CsrfSession csrfSession(
            MvcResult result,
            String originalSessionId,
            String rotatedSessionId
    ) throws Exception {
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        String body = result.getResponse().getContentAsString();
        return new CsrfSession(
                session,
                JsonPath.read(body, "$.headerName"),
                JsonPath.read(body, "$.token"),
                originalSessionId,
                rotatedSessionId
        );
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder signature(
            CsrfSession csrf,
            String remoteAddress
    ) {
        return post("/api/v1/admin/uploads/signature")
                .session(csrf.session())
                .header(csrf.headerName(), csrf.token())
                .with(request -> {
                    request.setRemoteAddr(remoteAddress);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(validUploadDeclaration());
    }

    private static String validUploadDeclaration() {
        return """
                {
                  "fileName": "security-test.jpg",
                  "contentType": "image/jpeg",
                  "bytes": 1024
                }
                """;
    }

    private record CsrfSession(
            MockHttpSession session,
            String headerName,
            String token,
            String originalSessionId,
            String rotatedSessionId
    ) {
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ClockConfiguration {

        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock(START);
        }
    }

    static final class MutableClock extends Clock {

        private volatile Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void set(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
