package com.tomrick.mygallery.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminCredentialsPropertiesTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(CredentialsConfiguration.class);

    @Test
    void blankRuntimeCredentialsFailFast() {
        contextRunner
                .withPropertyValues(
                        "mygallery.auth.admin.username=",
                        "mygallery.auth.admin.password-bcrypt-hash="
                )
                .run(context -> {
                    Throwable failure = context.getStartupFailure();
                    assertNotNull(failure);
                    assertTrue(failure.getMessage().contains("AdminCredentialsProperties"));
                });
    }

    @Test
    void credentialsToStringNeverRevealsConfiguredValues() {
        var properties = new AdminCredentialsProperties(
                "owner-name",
                "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"
        );

        assertTrue(properties.toString().contains("<redacted>"));
        assertTrue(!properties.toString().contains("owner-name"));
        assertTrue(!properties.toString().contains(properties.passwordBcryptHash()));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(AdminCredentialsProperties.class)
    static class CredentialsConfiguration {
    }
}
