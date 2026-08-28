package com.tomrick.mygallery.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.pathPattern;

@Component
public class AdminOriginValidationFilter extends OncePerRequestFilter {

    private static final RequestMatcher PROTECTED_MUTATIONS = new OrRequestMatcher(
            pathPattern(HttpMethod.POST, "/api/v1/admin/session"),
            pathPattern(HttpMethod.DELETE, "/api/v1/admin/session"),
            pathPattern(HttpMethod.POST, "/api/v1/admin/photos"),
            pathPattern(HttpMethod.PUT, "/api/v1/admin/photos/{id}"),
            pathPattern(HttpMethod.DELETE, "/api/v1/admin/photos/{id}"),
            pathPattern(HttpMethod.POST, "/api/v1/admin/uploads/signature")
    );

    private final AdminCorsPolicy corsPolicy;
    private final boolean enabled;

    public AdminOriginValidationFilter(
            AdminCorsPolicy corsPolicy,
            @Value("${mygallery.security.origin-enforcement-enabled:false}") boolean enabled
    ) {
        this.corsPolicy = corsPolicy;
        this.enabled = enabled;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled || !PROTECTED_MUTATIONS.matches(request);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        boolean allowed = origin != null
                ? corsPolicy.allowsOriginHeader(origin)
                : corsPolicy.allowsReferer(request.getHeader(HttpHeaders.REFERER));

        if (allowed) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"code\":\"ORIGIN_FORBIDDEN\",\"message\":\"Request origin is not allowed\"}"
        );
    }
}
