package com.tomrick.mygallery.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminLoginRequest(
        @NotBlank @Size(max = 200) String username,
        @NotBlank @Size(max = 200) String password
) {

    @Override
    public String toString() {
        return "AdminLoginRequest[username=<redacted>, password=<redacted>]";
    }
}
