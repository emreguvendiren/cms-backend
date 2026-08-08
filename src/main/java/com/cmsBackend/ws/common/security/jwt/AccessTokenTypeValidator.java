package com.cmsBackend.ws.common.security.jwt;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class AccessTokenTypeValidator implements OAuth2TokenValidator<Jwt> {
    private static final OAuth2Error ERROR = new OAuth2Error("invalid_token", "Invalid token type", null);

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        return "access".equals(jwt.getClaimAsString("token_type")) && jwt.getSubject() != null
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(ERROR);
    }
}
