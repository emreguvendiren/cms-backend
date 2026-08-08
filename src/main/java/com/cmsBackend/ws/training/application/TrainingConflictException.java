package com.cmsBackend.ws.training.application;

public class TrainingConflictException extends RuntimeException {
    public TrainingConflictException() {
        super("Training resource conflicts with an existing record.");
    }
}
