package com.cmsBackend.ws.auth.infrastructure.web;

import com.cmsBackend.ws.common.security.config.SecurityProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class RefreshCookieService {
    public static final String PATH = "/api/auth";
    private final SecurityProperties properties;

    public RefreshCookieService(SecurityProperties properties) {
        this.properties = properties;
    }

    public String name() { return properties.refreshCookieName(); }

    public String read(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (properties.refreshCookieName().equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }

    public void set(HttpHeaders headers, String token, boolean persistentSession) {
        ResponseCookie.ResponseCookieBuilder builder = cookie(token);
        if (persistentSession) builder.maxAge(properties.refreshTokenTtl());
        headers.add(HttpHeaders.SET_COOKIE, builder.build().toString());
    }

    public void clear(HttpHeaders headers) {
        headers.add(HttpHeaders.SET_COOKIE, cookie("").maxAge(Duration.ZERO).build().toString());
    }

    private ResponseCookie.ResponseCookieBuilder cookie(String value) {
        return ResponseCookie.from(properties.refreshCookieName(), value)
                .httpOnly(true)
                .secure(properties.refreshCookieSecure())
                .sameSite(properties.refreshCookieSameSite())
                .path(PATH);
    }
}
