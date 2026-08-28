package com.tomrick.mygallery.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("mygallery.auth.admin")
public record AdminCredentialsProperties(
        @NotBlank String username,
        @NotBlank
        @Pattern(
                regexp = "^\\$2[aby]\\$(?:0[4-9]|[12][0-9]|3[01])\\$[./A-Za-z0-9]{53}$",
                message = "must be a valid BCrypt hash"
        )
        String passwordBcryptHash
) {

    public AdminCredentialsProperties {
        username = username == null ? null : username.trim();
        passwordBcryptHash = passwordBcryptHash == null ? null : passwordBcryptHash.trim();
    }

    @Override
    public String toString() {
        return "AdminCredentialsProperties[username=<redacted>, passwordBcryptHash=<redacted>]";
    }
}
