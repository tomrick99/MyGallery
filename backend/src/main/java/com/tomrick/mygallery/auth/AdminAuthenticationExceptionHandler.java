package com.tomrick.mygallery.auth;

import com.tomrick.mygallery.auth.dto.AdminAuthErrorResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AdminSessionController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AdminAuthenticationExceptionHandler {

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<AdminAuthErrorResponse> handleInvalidLoginRequest(Exception exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .cacheControl(CacheControl.noStore())
                .body(new AdminAuthErrorResponse("VALIDATION_FAILED", "Request validation failed"));
    }
}
