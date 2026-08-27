package com.tomrick.mygallery.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Temporary foundation policy until the dedicated admin authentication feature is implemented.
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class FoundationSecurityConfiguration {

    @Bean
    SecurityFilterChain foundationSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                HttpMethod.GET,
                                "/actuator/health",
                                "/actuator/health/**",
                                "/api/v1/photos",
                                "/api/v1/photos/featured",
                                "/api/v1/photos/*",
                                "/api/v1/archive"
                        ).permitAll()
                        .anyRequest().denyAll())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable);

        return http.build();
    }
}
