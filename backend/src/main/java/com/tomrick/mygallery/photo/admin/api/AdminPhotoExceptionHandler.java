package com.tomrick.mygallery.photo.admin.api;

import com.tomrick.mygallery.photo.admin.api.dto.AdminPhotoErrorResponse;
import com.tomrick.mygallery.photo.admin.api.dto.AdminPhotoFieldErrorResponse;
import com.tomrick.mygallery.photo.admin.application.AssetDeleteFailedException;
import com.tomrick.mygallery.photo.admin.application.AdminPhotoNotFoundException;
import com.tomrick.mygallery.photo.admin.application.InvalidUploadDeclarationException;
import com.tomrick.mygallery.photo.admin.application.InvalidAdminPhotoFilterException;
import com.tomrick.mygallery.photo.admin.application.InvalidUploadedAssetException;
import com.tomrick.mygallery.photo.admin.application.MediaProviderUnavailableException;
import com.tomrick.mygallery.photo.admin.application.UploadRateLimitExceededException;
import com.tomrick.mygallery.photo.admin.domain.DuplicatePhotoAssetException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
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

@RestControllerAdvice(assignableTypes = {
        AdminPhotoController.class,
        AdminUploadController.class
})
public class AdminPhotoExceptionHandler {

    @ExceptionHandler(AdminPhotoNotFoundException.class)
    public ResponseEntity<AdminPhotoErrorResponse> handleNotFound() {
        return error(HttpStatus.NOT_FOUND, "PHOTO_NOT_FOUND", "Photo not found", List.of());
    }

    @ExceptionHandler(InvalidAdminPhotoFilterException.class)
    public ResponseEntity<AdminPhotoErrorResponse> handleInvalidFilter() {
        return error(HttpStatus.BAD_REQUEST, "INVALID_FILTER", "Invalid pagination", List.of());
    }

    @ExceptionHandler(InvalidUploadDeclarationException.class)
    public ResponseEntity<AdminPhotoErrorResponse> handleInvalidUploadDeclaration() {
        return error(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "Request validation failed",
                List.of()
        );
    }

    @ExceptionHandler(InvalidUploadedAssetException.class)
    public ResponseEntity<AdminPhotoErrorResponse> handleInvalidUploadedAsset() {
        return error(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "INVALID_UPLOADED_ASSET",
                "Uploaded asset could not be verified",
                List.of()
        );
    }

    @ExceptionHandler(DuplicatePhotoAssetException.class)
    public ResponseEntity<AdminPhotoErrorResponse> handleDuplicateAsset() {
        return error(
                HttpStatus.CONFLICT,
                "PHOTO_ASSET_ALREADY_LINKED",
                "Uploaded asset is already linked to a photo",
                List.of()
        );
    }

    @ExceptionHandler(MediaProviderUnavailableException.class)
    public ResponseEntity<AdminPhotoErrorResponse> handleMediaProviderUnavailable() {
        return error(
                HttpStatus.BAD_GATEWAY,
                "MEDIA_PROVIDER_UNAVAILABLE",
                "Media provider is temporarily unavailable",
                List.of()
        );
    }

    @ExceptionHandler(AssetDeleteFailedException.class)
    public ResponseEntity<AdminPhotoErrorResponse> handleAssetDeleteFailed() {
        return error(
                HttpStatus.BAD_GATEWAY,
                "ASSET_DELETE_FAILED",
                "Asset deletion failed",
                List.of()
        );
    }

    @ExceptionHandler(UploadRateLimitExceededException.class)
    public ResponseEntity<AdminPhotoErrorResponse> handleUploadRateLimit(
            UploadRateLimitExceededException exception
    ) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(exception.retryAfterSeconds()))
                .cacheControl(CacheControl.noStore())
                .body(new AdminPhotoErrorResponse(
                        "RATE_LIMITED",
                        "Too many upload requests",
                        List.of()
                ));
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
