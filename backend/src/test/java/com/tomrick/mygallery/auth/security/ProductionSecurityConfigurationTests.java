package com.tomrick.mygallery.auth.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionSecurityConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(SecurityHardeningConfiguration.class);

    @Test
    void secureProductionConfigurationIsValid() {
        productionRunner().run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void productionRejectsInsecureOrIncorrectSessionCookies() {
        productionRunner()
                .withPropertyValues("server.servlet.session.cookie.secure=false")
                .run(context -> assertThat(context).hasFailed());

        productionRunner()
                .withPropertyValues("server.servlet.session.cookie.name=mygallery-session")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void productionRejectsEmptyHttpOrUnsafeCorsOrigins() {
        for (String origins : List.of(
                "",
                "http://www.example.test",
                "https://*.vercel.app",
                "https://www.example.test/path",
                "https://user@www.example.test",
                "https://www.example.test?query=value",
                "https://www.example.test#fragment",
                "not-an-origin",
                "https://localhost:3000"
        )) {
            productionRunner()
                    .withPropertyValues("mygallery.auth.cors.allowed-origins=" + origins)
                    .run(context -> assertThat(context).hasFailed());
        }
    }

    @Test
    void productionRejectsNonPositiveAbsoluteSessionLifetime() {
        productionRunner()
                .withPropertyValues("mygallery.auth.session.absolute-timeout=0s")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void productionRejectsDisabledAdminOriginEnforcement() {
        productionRunner()
                .withPropertyValues("mygallery.security.origin-enforcement-enabled=false")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void developmentAllowsExplicitLocalHttpOriginAndInsecureCookie() {
        contextRunner
                .withPropertyValues(
                        "mygallery.auth.cors.allowed-origins=http://localhost:3000",
                        "mygallery.auth.session.absolute-timeout=8h",
                        "server.servlet.session.cookie.name=mygallery-session",
                        "server.servlet.session.cookie.secure=false"
                )
                .run(context -> assertThat(context).hasNotFailed());
    }

    private ApplicationContextRunner productionRunner() {
        return contextRunner.withPropertyValues(
                "spring.profiles.active=prod",
                "mygallery.auth.cors.allowed-origins=https://www.example.test",
                "mygallery.auth.session.absolute-timeout=8h",
                "mygallery.security.origin-enforcement-enabled=true",
                "server.servlet.session.cookie.name=__Host-mygallery-session",
                "server.servlet.session.cookie.secure=true",
                "server.servlet.session.cookie.http-only=true",
                "server.servlet.session.cookie.same-site=lax",
                "server.servlet.session.cookie.path=/",
                "server.servlet.session.cookie.domain="
        );
    }
}
