package com.cmsBackend.ws.training.api.model;

import com.cmsBackend.ws.training.domain.EnrollmentStatus;
import com.cmsBackend.ws.training.domain.PaymentMethod;
import com.cmsBackend.ws.training.domain.PaymentPlanType;
import com.cmsBackend.ws.training.domain.PaymentStatus;
import com.cmsBackend.ws.training.infrastructure.persistence.ClassEnrollmentJpaEntity;
import com.cmsBackend.ws.training.infrastructure.persistence.EnrollmentPaymentJpaEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public record ClassDetailResponse(ClassResponse classInfo, List<EnrolledStudentResponse> students) {
    public record EnrolledStudentResponse(UUID id, UUID enrollmentId, String fullName, String email, String phoneMasked,
            EnrollmentStatus enrollmentStatus, BigDecimal registrationFee, PaymentPlanType paymentPlan,
            Integer installmentCount, LocalDate firstPaymentDate, PaymentStatus paymentStatus,
            LocalDate expectedPaymentDate, String note, List<EnrollmentPaymentResponse> payments, long version) {
        public static EnrolledStudentResponse from(ClassEnrollmentJpaEntity enrollment) {
            var student=enrollment.getStudent();
            return new EnrolledStudentResponse(student.getId(), enrollment.getId(), student.getFullName(), student.getEmail(),
                    "••• ••• •• ••", enrollment.getStatus(), enrollment.getRegistrationFee(),
                    enrollment.getPaymentPlan(), enrollment.getInstallmentCount(), enrollment.getFirstPaymentDate(),
                    enrollment.getPaymentStatus(), enrollment.getExpectedPaymentDate(), enrollment.getNote(),
                    enrollment.getPayments().stream().map(EnrollmentPaymentResponse::from).toList(),
                    enrollment.getVersion());
        }
    }
    public record EnrollmentPaymentResponse(UUID id, int installmentNumber, int installmentTotal,
            BigDecimal amount, LocalDate dueDate, PaymentStatus status, LocalDate paidAt,
            PaymentMethod paymentMethod, long version) {
        public static EnrollmentPaymentResponse from(EnrollmentPaymentJpaEntity payment) {
            return new EnrollmentPaymentResponse(payment.getId(), payment.getInstallmentNumber(),
                    payment.getInstallmentTotal(), payment.getAmount(), payment.getDueDate(), payment.getStatus(),
                    payment.getPaidAt(), payment.getPaymentMethod(), payment.getVersion());
        }
    }
}
