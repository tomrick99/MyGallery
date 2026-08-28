package com.tomrick.mygallery.photo.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record UploadSignatureRequest(
        @NotBlank @Size(max = 255) String fileName,
        @NotBlank String contentType,
        @NotNull @Positive Long bytes
) {

    public UploadSignatureRequest {
        fileName = fileName == null ? null : fileName.trim();
        contentType = contentType == null ? null : contentType.trim().toLowerCase(Locale.ROOT);
    }
}
