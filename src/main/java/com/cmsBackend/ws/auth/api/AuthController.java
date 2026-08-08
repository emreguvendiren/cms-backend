package com.cmsBackend.ws.auth.api;

import com.cmsBackend.ws.auth.api.model.AccessTokenResponse;
import com.cmsBackend.ws.auth.api.model.CsrfResponse;
import com.cmsBackend.ws.auth.api.model.LoginRequest;
import com.cmsBackend.ws.auth.api.model.UserResponse;
import com.cmsBackend.ws.auth.application.AuthenticationFailureException;
import com.cmsBackend.ws.auth.application.AuthenticationResult;
import com.cmsBackend.ws.auth.application.CurrentUserService;
import com.cmsBackend.ws.auth.application.LoginService;
import com.cmsBackend.ws.auth.application.LogoutService;
import com.cmsBackend.ws.auth.application.TokenRefreshService;
import com.cmsBackend.ws.auth.infrastructure.web.RefreshCookieService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final LoginService loginService;
    private final TokenRefreshService tokenRefreshService;
    private final LogoutService logoutService;
    private final CurrentUserService currentUserService;
    private final RefreshCookieService cookies;

    public AuthController(
            LoginService loginService,
            TokenRefreshService tokenRefreshService,
            LogoutService logoutService,
            CurrentUserService currentUserService,
            RefreshCookieService cookies) {
        this.loginService = loginService;
        this.tokenRefreshService = tokenRefreshService;
        this.logoutService = logoutService;
        this.currentUserService = currentUserService;
        this.cookies = cookies;
    }

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken csrfToken) {
        return new CsrfResponse(csrfToken.getToken());
    }

    @PostMapping("/login")
    public ResponseEntity<AccessTokenResponse> login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        return tokenResponse(loginService.login(
                request.email(), request.password(), request.persistentSession(), servletRequest.getRemoteAddr()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AccessTokenResponse> refresh(HttpServletRequest servletRequest) {
        String refreshToken = cookies.read(servletRequest);
        if (refreshToken == null || refreshToken.isBlank()) throw new AuthenticationFailureException();
        return tokenResponse(tokenRefreshService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest servletRequest) {
        logoutService.logout(cookies.read(servletRequest));
        HttpHeaders headers = noStoreHeaders();
        cookies.clear(headers);
        return ResponseEntity.noContent().headers(headers).build();
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return UserResponse.from(currentUserService.currentUser(UUID.fromString(jwt.getSubject())));
    }

    private ResponseEntity<AccessTokenResponse> tokenResponse(AuthenticationResult result) {
        HttpHeaders headers = noStoreHeaders();
        cookies.set(headers, result.refreshToken(), result.persistentSession());
        var response = new AccessTokenResponse(
                result.accessToken(),
                "Bearer",
                result.accessTokenExpiresAt(),
                UserResponse.from(result.user()));
        return ResponseEntity.ok().headers(headers).body(response);
    }

    private HttpHeaders noStoreHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.noStore());
        headers.setPragma("no-cache");
        return headers;
    }
}
