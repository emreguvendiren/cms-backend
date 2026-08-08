package com.cmsBackend.ws.training.application;

public class TrainingNotFoundException extends RuntimeException {
    public TrainingNotFoundException() {
        super("Training resource was not found.");
    }
}
