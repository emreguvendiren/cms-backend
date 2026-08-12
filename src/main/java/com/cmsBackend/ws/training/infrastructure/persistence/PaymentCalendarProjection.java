package com.cmsBackend.ws.training.infrastructure.persistence;

import com.cmsBackend.ws.training.domain.PaymentMethod;
import com.cmsBackend.ws.training.domain.PaymentPlanType;
import com.cmsBackend.ws.training.domain.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public interface PaymentCalendarProjection {
    UUID getPaymentId();
    UUID getEnrollmentId();
    UUID getClassId();
    String getClassCode();
    String getClassName();
    String getCourseName();
    UUID getStudentId();
    String getStudentFullName();
    PaymentPlanType getPaymentPlan();
    int getInstallmentNumber();
    int getInstallmentTotal();
    BigDecimal getAmount();
    LocalDate getDueDate();
    PaymentStatus getStatus();
    LocalDate getPaidAt();
    PaymentMethod getPaymentMethod();
}
