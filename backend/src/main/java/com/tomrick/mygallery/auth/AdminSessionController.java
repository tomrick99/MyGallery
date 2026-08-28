package com.tomrick.mygallery.auth;

import com.tomrick.mygallery.auth.dto.AdminAuthErrorResponse;
import com.tomrick.mygallery.auth.dto.AdminLoginRequest;
import com.tomrick.mygallery.auth.dto.AdminSessionResponse;
import com.tomrick.mygallery.auth.dto.CsrfResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminSessionController {

    private final AdminAuthenticationService authenticationService;

    public AdminSessionController(AdminAuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @GetMapping("/csrf")
    public ResponseEntity<CsrfResponse> csrf(CsrfToken csrfToken) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new CsrfResponse(csrfToken.getHeaderName(), csrfToken.getToken()));
    }

    @PostMapping("/session")
    public ResponseEntity<?> login(
            @Valid @RequestBody AdminLoginRequest loginRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        var result = authenticationService.login(
                loginRequest.username(),
                loginRequest.password(),
                request,
                response
        );

        return switch (result.status()) {
            case AUTHENTICATED -> ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .body(new AdminSessionResponse(true, result.username()));
            case INVALID_CREDENTIALS -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .cacheControl(CacheControl.noStore())
                    .body(new AdminAuthErrorResponse("INVALID_CREDENTIALS", "Invalid credentials"));
            case RATE_LIMITED -> ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header(HttpHeaders.RETRY_AFTER, Long.toString(result.retryAfterSeconds()))
                    .cacheControl(CacheControl.noStore())
                    .body(new AdminAuthErrorResponse("RATE_LIMITED", "Too many login attempts"));
        };
    }

    @GetMapping("/session")
    public ResponseEntity<AdminSessionResponse> session(Authentication authentication) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new AdminSessionResponse(true, authentication.getName()));
    }

    @DeleteMapping("/session")
    public ResponseEntity<Void> logout(
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        authenticationService.logout(authentication, request, response);
        return ResponseEntity.noContent()
                .cacheControl(CacheControl.noStore())
                .build();
    }
}
