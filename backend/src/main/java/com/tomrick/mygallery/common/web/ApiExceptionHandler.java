package com.tomrick.mygallery.common.web;

import com.tomrick.mygallery.photo.application.PhotoNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.tomrick.mygallery")
@Order(Ordered.LOWEST_PRECEDENCE)
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(PhotoNotFoundException.class)
    public ResponseEntity<Void> handlePublicPhotoNotFound() {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
            Exception exception,
            HttpServletRequest request
    ) {
        String requestId = RequestCorrelationFilter.requestId(request);
        log.error(
                "Unexpected API error: requestId={}, exceptionType={}",
                requestId,
                exception.getClass().getName()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .cacheControl(CacheControl.noStore())
                .body(new ApiErrorResponse(
                        "INTERNAL_ERROR",
                        "An unexpected error occurred",
                        requestId
                ));
    }
}
