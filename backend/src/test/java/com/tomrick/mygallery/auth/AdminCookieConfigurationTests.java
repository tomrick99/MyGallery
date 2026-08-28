package com.tomrick.mygallery.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.Cookie;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.boot.web.server.servlet.Session;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "server.servlet.session.cookie.name=__Host-mygallery-session",
        "server.servlet.session.cookie.secure=true"
})
class AdminCookieConfigurationTests {

    @Autowired
    private ServerProperties serverProperties;

    @Test
    void productionCookieTargetIsSecureHttpOnlyHostOnlyAndLax() {
        var session = serverProperties.getServlet().getSession();
        var cookie = session.getCookie();

        assertEquals(Duration.ofMinutes(30), session.getTimeout());
        assertEquals(java.util.Set.of(Session.SessionTrackingMode.COOKIE), session.getTrackingModes());
        assertEquals("__Host-mygallery-session", cookie.getName());
        assertTrue(cookie.getSecure());
        assertTrue(cookie.getHttpOnly());
        assertEquals("/", cookie.getPath());
        assertEquals(Cookie.SameSite.LAX, cookie.getSameSite());
        assertNull(cookie.getDomain());
    }
}
