package com.cmsBackend.ws.training.api;

import com.cmsBackend.ws.common.security.web.ApiError;
import com.cmsBackend.ws.training.application.TrainingConflictException;
import com.cmsBackend.ws.training.application.TrainingNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice
public class TrainingExceptionHandler {
    @ExceptionHandler(TrainingNotFoundException.class)
    ResponseEntity<ApiError> notFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of("TRAINING_RESOURCE_NOT_FOUND", "The requested training resource was not found."));
    }

    @ExceptionHandler({TrainingConflictException.class, DataIntegrityViolationException.class, ObjectOptimisticLockingFailureException.class})
    ResponseEntity<ApiError> conflict() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of("TRAINING_RESOURCE_CONFLICT", "The training record conflicts with existing data."));
    }

    @ExceptionHandler({IllegalArgumentException.class, HandlerMethodValidationException.class, ConstraintViolationException.class})
    ResponseEntity<ApiError> invalidRequest() {
        return ResponseEntity.badRequest().body(ApiError.of("VALIDATION_FAILED", "The request is invalid."));
    }
}
