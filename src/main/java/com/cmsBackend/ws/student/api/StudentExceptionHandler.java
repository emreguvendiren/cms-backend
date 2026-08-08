package com.cmsBackend.ws.student.api;

import com.cmsBackend.ws.common.security.web.ApiError;
import com.cmsBackend.ws.student.application.*;
import com.cmsBackend.ws.student.infrastructure.crypto.SensitiveDataProtectionException;
import java.time.Instant;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = StudentController.class)
public class StudentExceptionHandler {
    @ExceptionHandler(StudentNotFoundException.class)
    ResponseEntity<ApiError> notFound() { return response(HttpStatus.NOT_FOUND, "STUDENT_NOT_FOUND", "Student was not found."); }
    @ExceptionHandler(StudentConflictException.class)
    ResponseEntity<ApiError> conflict() { return response(HttpStatus.CONFLICT, "STUDENT_CONFLICT", "Student data conflicts with an existing record."); }
    @ExceptionHandler(StudentValidationException.class)
    ResponseEntity<ApiError> validation(StudentValidationException ex) { return response(HttpStatus.BAD_REQUEST, "STUDENT_VALIDATION_FAILED", ex.getMessage()); }
    @ExceptionHandler(SensitiveDataProtectionException.class)
    ResponseEntity<ApiError> protection() { return response(HttpStatus.INTERNAL_SERVER_ERROR, "SENSITIVE_DATA_OPERATION_FAILED", "Sensitive data operation failed."); }
    private ResponseEntity<ApiError> response(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ApiError(code, message, Instant.now()));
    }
}
