package com.tomrick.mygallery.auth.security;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
public class SecurityHardeningConfiguration {

    @Bean
    Clock adminSecurityClock() {
        return Clock.systemUTC();
    }

    @Bean
    AdminCorsPolicy adminCorsPolicy(
            @Value("${mygallery.auth.cors.allowed-origins:}") String configuredOrigins
    ) {
        return new AdminCorsPolicy(configuredOrigins);
    }

    @Bean
    AdminSessionPolicy adminSessionPolicy(
            @Value("${mygallery.auth.session.absolute-timeout:8h}") String absoluteTimeout
    ) {
        return new AdminSessionPolicy(DurationStyle.detectAndParse(absoluteTimeout));
    }

    @Bean
    @Profile("prod")
    InitializingBean productionSecurityValidator(
            AdminCorsPolicy corsPolicy,
            AdminSessionPolicy sessionPolicy,
            @Value("${server.servlet.session.cookie.name}") String cookieName,
            @Value("${server.servlet.session.cookie.secure}") boolean cookieSecure,
            @Value("${server.servlet.session.cookie.http-only:true}") boolean cookieHttpOnly,
            @Value("${server.servlet.session.cookie.same-site:lax}") String cookieSameSite,
            @Value("${server.servlet.session.cookie.path:/}") String cookiePath,
            @Value("${server.servlet.session.cookie.domain:}") String cookieDomain,
            @Value("${mygallery.security.origin-enforcement-enabled:false}")
            boolean originEnforcementEnabled
    ) {
        return () -> {
            if (!"__Host-mygallery-session".equals(cookieName)) {
                throw new IllegalStateException(
                        "Production session cookie name must be __Host-mygallery-session"
                );
            }
            if (!cookieSecure) {
                throw new IllegalStateException("Production session cookie must be Secure");
            }
            if (!cookieHttpOnly
                    || !"lax".equalsIgnoreCase(cookieSameSite)
                    || !"/".equals(cookiePath)
                    || (cookieDomain != null && !cookieDomain.isBlank())) {
                throw new IllegalStateException(
                        "Production session cookie must be HttpOnly, SameSite=Lax, Path=/, and host-only"
                );
            }
            if (corsPolicy.allowedOrigins().isEmpty()) {
                throw new IllegalStateException("Production CORS allowlist must not be empty");
            }
            if (!corsPolicy.containsOnlyHttpsOrigins()) {
                throw new IllegalStateException("Production CORS origins must use HTTPS");
            }
            if (corsPolicy.containsLocalhostOrigin()) {
                throw new IllegalStateException("Production CORS origins must not use localhost");
            }
            if (sessionPolicy.absoluteTimeout() == null
                    || sessionPolicy.absoluteTimeout().isZero()
                    || sessionPolicy.absoluteTimeout().isNegative()) {
                throw new IllegalStateException(
                        "Production absolute admin session lifetime must be positive"
                );
            }
            if (!originEnforcementEnabled) {
                throw new IllegalStateException(
                        "Production admin mutation origin enforcement must be enabled"
                );
            }
        };
    }
}
