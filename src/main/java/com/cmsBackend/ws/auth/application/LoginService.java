package com.cmsBackend.ws.auth.application;

import com.cmsBackend.ws.common.security.audit.SecurityAuditService;
import com.cmsBackend.ws.user.domain.UserAccount;
import com.cmsBackend.ws.user.domain.UserAccountRepository;
import java.util.UUID;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
public class LoginService {
    private final AuthenticationManager authenticationManager;
    private final UserAccountRepository users;
    private final TokenSessionIssuer tokenSessionIssuer;
    private final AuthenticationRateLimiter rateLimiter;
    private final SecurityAuditService audit;

    public LoginService(
            AuthenticationManager authenticationManager,
            UserAccountRepository users,
            TokenSessionIssuer tokenSessionIssuer,
            AuthenticationRateLimiter rateLimiter,
            SecurityAuditService audit) {
        this.authenticationManager = authenticationManager;
        this.users = users;
        this.tokenSessionIssuer = tokenSessionIssuer;
        this.rateLimiter = rateLimiter;
        this.audit = audit;
    }

    public AuthenticationResult login(String email, String password, boolean persistentSession, String clientKey) {
        String normalizedEmail = email.trim().toLowerCase();
        String limitKey = "login:" + clientKey + ":" + normalizedEmail;
        if (!rateLimiter.allow(limitKey)) {
            audit.loginRateLimited();
            throw new AuthenticationFailureException();
        }
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(normalizedEmail, password));
        } catch (AuthenticationException exception) {
            audit.loginFailed();
            throw new AuthenticationFailureException();
        }
        UserAccount user = users.findByEmail(normalizedEmail).orElseThrow(AuthenticationFailureException::new);
        rateLimiter.clear(limitKey);
        AuthenticationResult result = tokenSessionIssuer.issue(user, UUID.randomUUID(), persistentSession);
        audit.loginSucceeded(user.id());
        return result;
    }
}
