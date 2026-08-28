package com.tomrick.mygallery.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Component
public class AdminSessionLifetimeFilter extends OncePerRequestFilter {

    public static final String AUTHENTICATED_AT_ATTRIBUTE =
            AdminSessionLifetimeFilter.class.getName() + ".AUTHENTICATED_AT";

    private final AdminSessionPolicy sessionPolicy;
    private final Clock clock;
    private final LogoutHandler logoutHandler;

    public AdminSessionLifetimeFilter(
            AdminSessionPolicy sessionPolicy,
            Clock clock,
            LogoutHandler logoutHandler
    ) {
        this.sessionPolicy = sessionPolicy;
        this.clock = clock;
        this.logoutHandler = logoutHandler;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = requestPath(request);
        return !path.startsWith("/api/v1/admin/")
                || "/api/v1/admin/csrf".equals(path)
                || ("POST".equals(request.getMethod())
                    && "/api/v1/admin/session".equals(path));
    }

    private static String requestPath(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        return contextPath.isEmpty() ? requestUri : requestUri.substring(contextPath.length());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!isAuthenticatedAdmin(authentication)) {
            filterChain.doFilter(request, response);
            return;
        }

        HttpSession session = request.getSession(false);
        Instant authenticatedAt = session == null
                ? null
                : authenticatedAt(session.getAttribute(AUTHENTICATED_AT_ATTRIBUTE));
        if (authenticatedAt != null && !isExpired(authenticatedAt)) {
            filterChain.doFilter(request, response);
            return;
        }

        logoutHandler.logout(request, response, authentication);
        SecurityContextHolder.clearContext();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"code\":\"SESSION_EXPIRED\",\"message\":\"Authentication required\"}"
        );
    }

    private boolean isExpired(Instant authenticatedAt) {
        Duration authenticatedFor = Duration.between(authenticatedAt, clock.instant());
        return authenticatedFor.isNegative()
                || authenticatedFor.compareTo(sessionPolicy.absoluteTimeout()) > 0;
    }

    private static Instant authenticatedAt(Object value) {
        if (value instanceof Instant instant) {
            return instant;
        }
        return null;
    }

    private static boolean isAuthenticatedAdmin(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
