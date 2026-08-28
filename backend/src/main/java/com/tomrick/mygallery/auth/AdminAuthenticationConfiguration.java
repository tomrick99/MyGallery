package com.tomrick.mygallery.auth;

import com.tomrick.mygallery.auth.security.JsonAccessDeniedHandler;
import com.tomrick.mygallery.auth.security.JsonAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.CompositeLogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfLogoutHandler;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import static org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.pathPattern;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableConfigurationProperties(AdminCredentialsProperties.class)
public class AdminAuthenticationConfiguration {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService adminUserDetailsService(AdminCredentialsProperties credentials) {
        var admin = User.withUsername(credentials.username())
                .password(credentials.passwordBcryptHash())
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(admin);
    }

    @Bean
    AuthenticationManager authenticationManager(
            UserDetailsService adminUserDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        var provider = new DaoAuthenticationProvider(adminUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    CsrfTokenRepository csrfTokenRepository() {
        return new HttpSessionCsrfTokenRepository();
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        var repository = new HttpSessionSecurityContextRepository();
        repository.setDisableUrlRewriting(true);
        return repository;
    }

    @Bean
    SessionAuthenticationStrategy adminSessionAuthenticationStrategy(
            CsrfTokenRepository csrfTokenRepository
    ) {
        return new CompositeSessionAuthenticationStrategy(List.of(
                new ChangeSessionIdAuthenticationStrategy(),
                new CsrfAuthenticationStrategy(csrfTokenRepository)
        ));
    }

    @Bean
    LogoutHandler adminLogoutHandler(
            CsrfTokenRepository csrfTokenRepository,
            SecurityContextRepository securityContextRepository,
            @Value("${server.servlet.session.cookie.name}") String cookieName,
            @Value("${server.servlet.session.cookie.secure}") boolean cookieSecure
    ) {
        if (cookieName.startsWith("__Host-") && !cookieSecure) {
            throw new IllegalArgumentException("A __Host- session cookie must be Secure");
        }

        var securityContextLogoutHandler = new SecurityContextLogoutHandler();
        securityContextLogoutHandler.setSecurityContextRepository(securityContextRepository);

        LogoutHandler expireSessionCookie = (request, response, authentication) -> {
            var expiredCookie = ResponseCookie.from(cookieName, "")
                    .httpOnly(true)
                    .secure(cookieSecure)
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(Duration.ZERO)
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, expiredCookie.toString());
        };

        return new CompositeLogoutHandler(
                new CsrfLogoutHandler(csrfTokenRepository),
                securityContextLogoutHandler,
                expireSessionCookie
        );
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${mygallery.auth.cors.allowed-origins:}") String configuredOrigins
    ) {
        var configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(parseExactOrigins(configuredOrigins));
        configuration.setAllowCredentials(true);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Content-Type", "X-CSRF-TOKEN"));
        configuration.setExposedHeaders(List.of("Retry-After"));
        configuration.setMaxAge(Duration.ofHours(1));

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    SecurityFilterChain adminSecurityFilterChain(
            HttpSecurity http,
            CsrfTokenRepository csrfTokenRepository,
            SecurityContextRepository securityContextRepository,
            SessionAuthenticationStrategy adminSessionAuthenticationStrategy,
            CorsConfigurationSource corsConfigurationSource,
            JsonAuthenticationEntryPoint authenticationEntryPoint,
            JsonAccessDeniedHandler accessDeniedHandler
    ) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                pathPattern(HttpMethod.GET, "/actuator/health"),
                                pathPattern(HttpMethod.GET, "/actuator/health/**"),
                                pathPattern(HttpMethod.GET, "/api/v1/photos"),
                                pathPattern(HttpMethod.GET, "/api/v1/photos/featured"),
                                pathPattern(HttpMethod.GET, "/api/v1/photos/{id}"),
                                pathPattern(HttpMethod.GET, "/api/v1/archive"),
                                pathPattern(HttpMethod.GET, "/api/v1/admin/csrf"),
                                pathPattern(HttpMethod.POST, "/api/v1/admin/session")
                        ).permitAll()
                        .requestMatchers(
                                pathPattern(HttpMethod.GET, "/api/v1/admin/session"),
                                pathPattern(HttpMethod.DELETE, "/api/v1/admin/session"),
                                pathPattern("/api/v1/admin/photos/**"),
                                pathPattern(HttpMethod.POST, "/api/v1/admin/uploads/signature")
                        ).hasRole("ADMIN")
                        .anyRequest().denyAll())
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository))
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .securityContext(context -> context
                        .securityContextRepository(securityContextRepository)
                        .requireExplicitSave(true))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionAuthenticationStrategy(adminSessionAuthenticationStrategy))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .requestCache(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .rememberMe(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable);

        return http.build();
    }

    private static List<String> parseExactOrigins(String configuredOrigins) {
        var origins = Arrays.stream(configuredOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .peek(AdminAuthenticationConfiguration::validateExactOrigin)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        return List.copyOf(origins);
    }

    private static void validateExactOrigin(String origin) {
        if (origin.contains("*")) {
            throw new IllegalArgumentException("CORS_ALLOWED_ORIGINS must contain exact origins only");
        }

        URI uri;
        try {
            uri = URI.create(origin);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("CORS_ALLOWED_ORIGINS contains an invalid origin", exception);
        }

        boolean validScheme = "http".equalsIgnoreCase(uri.getScheme())
                || "https".equalsIgnoreCase(uri.getScheme());
        boolean hasOnlyOriginComponents = uri.getHost() != null
                && uri.getUserInfo() == null
                && (uri.getPath() == null || uri.getPath().isEmpty())
                && uri.getQuery() == null
                && uri.getFragment() == null;
        if (!validScheme || !hasOnlyOriginComponents) {
            throw new IllegalArgumentException("CORS_ALLOWED_ORIGINS must contain exact HTTP(S) origins");
        }
    }
}
