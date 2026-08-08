package com.cmsBackend.ws.auth.application;

public class AuthenticationFailureException extends RuntimeException {
    public AuthenticationFailureException() { super("Authentication failed"); }
}
