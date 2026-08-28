package com.tomrick.mygallery.auth;

import com.tomrick.mygallery.auth.rate.AdminLoginRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

@Service
public class AdminAuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AdminAuthenticationService.class);

    private final AuthenticationManager authenticationManager;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final SecurityContextRepository securityContextRepository;
    private final AdminLoginRateLimiter rateLimiter;
    private final LogoutHandler logoutHandler;

    public AdminAuthenticationService(
            AuthenticationManager authenticationManager,
            SessionAuthenticationStrategy sessionAuthenticationStrategy,
            SecurityContextRepository securityContextRepository,
            AdminLoginRateLimiter rateLimiter,
            LogoutHandler logoutHandler
    ) {
        this.authenticationManager = authenticationManager;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
        this.securityContextRepository = securityContextRepository;
        this.rateLimiter = rateLimiter;
        this.logoutHandler = logoutHandler;
    }

    public LoginResult login(
            String username,
            String password,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String rateLimitKey = rateLimiter.keyFor(request.getRemoteAddr(), username);
        var currentStatus = rateLimiter.status(rateLimitKey);
        if (currentStatus.blocked()) {
            log.warn("Admin login rate limit active");
            return LoginResult.rateLimited(currentStatus.retryAfterSeconds());
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(username, password)
            );
        } catch (AuthenticationException exception) {
            log.warn("Admin login failed");
            var failureStatus = rateLimiter.recordFailure(rateLimitKey);
            if (failureStatus.blocked()) {
                log.warn("Admin login rate limit activated");
                return LoginResult.rateLimited(failureStatus.retryAfterSeconds());
            }
            return LoginResult.invalidCredentials();
        }

        sessionAuthenticationStrategy.onAuthentication(authentication, request, response);
        var securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
        securityContextRepository.saveContext(securityContext, request, response);
        rateLimiter.reset(rateLimitKey);
        log.info("Admin login succeeded");
        return LoginResult.authenticated(authentication.getName());
    }

    public void logout(
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        logoutHandler.logout(request, response, authentication);
        log.info("Admin logout succeeded");
    }

    public enum LoginStatus {
        AUTHENTICATED,
        INVALID_CREDENTIALS,
        RATE_LIMITED
    }

    public record LoginResult(LoginStatus status, String username, long retryAfterSeconds) {

        static LoginResult authenticated(String username) {
            return new LoginResult(LoginStatus.AUTHENTICATED, username, 0);
        }

        static LoginResult invalidCredentials() {
            return new LoginResult(LoginStatus.INVALID_CREDENTIALS, null, 0);
        }

        static LoginResult rateLimited(long retryAfterSeconds) {
            return new LoginResult(LoginStatus.RATE_LIMITED, null, retryAfterSeconds);
        }
    }
}
