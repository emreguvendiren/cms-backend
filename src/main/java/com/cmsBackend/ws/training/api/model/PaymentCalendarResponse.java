package com.cmsBackend.ws.training.api.model;

import com.cmsBackend.ws.training.domain.PaymentMethod;
import com.cmsBackend.ws.training.domain.PaymentPlanType;
import com.cmsBackend.ws.training.domain.PaymentStatus;
import com.cmsBackend.ws.training.infrastructure.persistence.PaymentCalendarProjection;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PaymentCalendarResponse(String month, List<PaymentCalendarItemResponse> items) {
    public record PaymentCalendarItemResponse(UUID paymentId, UUID enrollmentId, UUID classId,
            String classCode, String className, String courseName, UUID studentId, String studentFullName,
            PaymentPlanType paymentPlan, int installmentNumber, int installmentTotal, BigDecimal amount,
            LocalDate dueDate, PaymentStatus status, LocalDate paidAt, PaymentMethod paymentMethod) {
        public static PaymentCalendarItemResponse from(PaymentCalendarProjection payment) {
            return new PaymentCalendarItemResponse(payment.getPaymentId(), payment.getEnrollmentId(),
                    payment.getClassId(), payment.getClassCode(), payment.getClassName(), payment.getCourseName(),
                    payment.getStudentId(), payment.getStudentFullName(), payment.getPaymentPlan(),
                    payment.getInstallmentNumber(), payment.getInstallmentTotal(), payment.getAmount(),
                    payment.getDueDate(), payment.getStatus(), payment.getPaidAt(), payment.getPaymentMethod());
        }
    }
}
