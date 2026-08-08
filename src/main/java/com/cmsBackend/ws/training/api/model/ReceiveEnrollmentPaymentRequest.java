package com.cmsBackend.ws.training.api.model;

import jakarta.validation.constraints.Min;

public record ReceiveEnrollmentPaymentRequest(@Min(0) long version) {}
