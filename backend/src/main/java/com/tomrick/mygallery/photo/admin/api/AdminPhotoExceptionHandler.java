package com.tomrick.mygallery.photo.admin.api;

import com.tomrick.mygallery.photo.admin.api.dto.AdminPhotoErrorResponse;
import com.tomrick.mygallery.photo.admin.api.dto.AdminPhotoFieldErrorResponse;
import com.tomrick.mygallery.photo.admin.application.AdminPhotoNotFoundException;
import com.tomrick.mygallery.photo.admin.application.InvalidAdminPhotoFilterException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Comparator;
import java.util.List;

@RestControllerAdvice(assignableTypes = AdminPhotoController.class)
public class AdminPhotoExceptionHandler {

    @ExceptionHandler(AdminPhotoNotFoundException.class)
    public ResponseEntity<AdminPhotoErrorResponse> handleNotFound() {
        return error(HttpStatus.NOT_FOUND, "PHOTO_NOT_FOUND", "Photo not found", List.of());
    }

    @ExceptionHandler(InvalidAdminPhotoFilterException.class)
    public ResponseEntity<AdminPhotoErrorResponse> handleInvalidFilter() {
        return error(HttpStatus.BAD_REQUEST, "INVALID_FILTER", "Invalid pagination", List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AdminPhotoErrorResponse> handleValidation(
            MethodArgumentNotValidException exception
    ) {
        List<AdminPhotoFieldErrorResponse> fieldErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .sorted(Comparator.comparing(FieldError::getField))
                .map(fieldError -> new AdminPhotoFieldErrorResponse(
                        fieldError.getField(),
                        fieldError.getCode(),
                        fieldError.getDefaultMessage()
                ))
                .toList();
        return error(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "Request validation failed",
                fieldErrors
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<AdminPhotoErrorResponse> handleUnreadableBody() {
        return error(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "Request validation failed",
                List.of()
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<AdminPhotoErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception
    ) {
        boolean paginationParameter = "page".equals(exception.getName())
                || "size".equals(exception.getName());
        return error(
                HttpStatus.BAD_REQUEST,
                paginationParameter ? "INVALID_FILTER" : "VALIDATION_FAILED",
                paginationParameter ? "Invalid pagination" : "Request validation failed",
                List.of()
        );
    }

    private static ResponseEntity<AdminPhotoErrorResponse> error(
            HttpStatus status,
            String code,
            String message,
            List<AdminPhotoFieldErrorResponse> fieldErrors
    ) {
        return ResponseEntity.status(status)
                .cacheControl(CacheControl.noStore())
                .body(new AdminPhotoErrorResponse(code, message, fieldErrors));
    }
}
