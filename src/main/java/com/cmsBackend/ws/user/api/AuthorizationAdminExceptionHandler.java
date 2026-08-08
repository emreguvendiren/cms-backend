package com.cmsBackend.ws.user.api;

import com.cmsBackend.ws.common.security.web.ApiError;
import com.cmsBackend.ws.user.application.SelfPermissionRemovalException;
import com.cmsBackend.ws.user.application.UserNotFoundException;
import com.cmsBackend.ws.user.application.UnknownAuthorityException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice(assignableTypes = AuthorizationAdminController.class)
public class AuthorizationAdminExceptionHandler {
    @ExceptionHandler(UserNotFoundException.class)
    ResponseEntity<ApiError> notFound() { return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of("USER_NOT_FOUND", "User was not found.")); }
    @ExceptionHandler(SelfPermissionRemovalException.class)
    ResponseEntity<ApiError> selfLockout() { return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of("SELF_PERMISSION_REMOVAL", "You cannot remove your own permission-management authority.")); }
    @ExceptionHandler(UnknownAuthorityException.class)
    ResponseEntity<ApiError> invalid() { return ResponseEntity.badRequest().body(ApiError.of("INVALID_AUTHORITY", "The authority selection is invalid.")); }
}
