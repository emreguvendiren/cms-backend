package com.cmsBackend.ws.auth.api;

import com.cmsBackend.ws.auth.application.AuthenticationFailureException;
import com.cmsBackend.ws.common.security.web.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AuthExceptionHandler {
    @ExceptionHandler(AuthenticationFailureException.class)
    ResponseEntity<ApiError> authenticationFailure() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.of("AUTHENTICATION_FAILED", "Email or password is invalid, or the session is unavailable."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validationFailure() {
        return ResponseEntity.badRequest().body(ApiError.of("VALIDATION_FAILED", "The request is invalid."));
    }
}
