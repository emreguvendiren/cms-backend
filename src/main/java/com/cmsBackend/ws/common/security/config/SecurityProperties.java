package com.cmsBackend.ws.common.security.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
        Jwt jwt,
        Duration refreshTokenTtl,
        String refreshCookieName,
        boolean refreshCookieSecure,
        String refreshCookieSameSite,
        @NotEmpty List<@NotBlank String> allowedOrigins) {

    public record Jwt(
            @NotBlank String issuer,
            @NotBlank String audience,
            Duration accessTokenTtl,
            String privateKey,
            String publicKey) {}

    @AssertTrue(message = "security token lifetimes must be positive")
    public boolean hasValidTokenLifetimes() {
        return positive(jwt.accessTokenTtl()) && positive(refreshTokenTtl);
    }

    @AssertTrue(message = "credentialed CORS origins must be exact HTTP(S) origins")
    public boolean hasSafeOrigins() {
        return allowedOrigins != null
                && allowedOrigins.stream()
                        .allMatch(origin -> (origin.startsWith("https://") || origin.startsWith("http://localhost:"))
                                && !origin.contains("*"));
    }

    @AssertTrue(message = "refresh cookie SameSite must be Strict or Lax; None additionally requires Secure")
    public boolean hasSafeCookiePolicy() {
        if (refreshCookieSameSite == null) return false;
        if (refreshCookieSameSite.equals("Strict") || refreshCookieSameSite.equals("Lax")) return true;
        return refreshCookieSameSite.equals("None") && refreshCookieSecure;
    }

    private static boolean positive(Duration duration) {
        return duration != null && !duration.isZero() && !duration.isNegative();
    }
}
