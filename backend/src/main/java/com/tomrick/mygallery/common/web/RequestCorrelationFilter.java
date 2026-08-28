package com.tomrick.mygallery.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestCorrelationFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Request-ID";
    public static final String REQUEST_ATTRIBUTE =
            RequestCorrelationFilter.class.getName() + ".REQUEST_ID";
    private static final String MDC_KEY = "requestId";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = contextPath.isEmpty()
                ? requestUri
                : requestUri.substring(contextPath.length());
        return !path.startsWith("/api/v1/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();
        request.setAttribute(REQUEST_ATTRIBUTE, requestId);
        response.setHeader(HEADER_NAME, requestId);
        MDC.put(MDC_KEY, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    public static String requestId(HttpServletRequest request) {
        Object requestId = request.getAttribute(REQUEST_ATTRIBUTE);
        return requestId instanceof String value ? value : "unavailable";
    }
}
