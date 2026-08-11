package com.cmsBackend.ws.training.api.model;

import com.cmsBackend.ws.training.domain.PaymentMethod;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ReceiveEnrollmentPaymentRequest(@Min(0) long version, @NotNull LocalDate paidAt,
        @NotNull PaymentMethod paymentMethod) {}
