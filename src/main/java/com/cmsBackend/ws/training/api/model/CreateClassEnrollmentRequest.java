package com.cmsBackend.ws.training.api.model;

import com.cmsBackend.ws.training.domain.PaymentPlanType;
import com.cmsBackend.ws.training.domain.PaymentStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateClassEnrollmentRequest(
        @NotNull UUID studentId,
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal registrationFee,
        @NotNull PaymentPlanType paymentPlan,
        @Min(2) @Max(24) Integer installmentCount,
        LocalDate firstPaymentDate,
        @NotNull PaymentStatus paymentStatus,
        LocalDate expectedPaymentDate,
        @Size(max = 1000) String note) {}
