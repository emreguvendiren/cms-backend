package com.cmsBackend.ws.training.api.model;

import com.cmsBackend.ws.training.domain.EnrollmentStatus;
import com.cmsBackend.ws.training.domain.PaymentMethod;
import com.cmsBackend.ws.training.domain.PaymentPlanType;
import com.cmsBackend.ws.training.domain.PaymentStatus;
import com.cmsBackend.ws.training.infrastructure.persistence.ClassEnrollmentJpaEntity;
import com.cmsBackend.ws.training.infrastructure.persistence.EnrollmentPaymentJpaEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ClassDetailResponse(ClassResponse classInfo, List<EnrolledStudentResponse> students) {
    public record EnrolledStudentResponse(UUID id, UUID enrollmentId, String fullName, String email, String phoneMasked,
            EnrollmentStatus enrollmentStatus, BigDecimal registrationFee, PaymentPlanType paymentPlan,
            Integer installmentCount, LocalDate firstPaymentDate, PaymentStatus paymentStatus,
            LocalDate expectedPaymentDate, String note, List<EnrollmentPaymentResponse> payments, long version) {
        public static EnrolledStudentResponse from(ClassEnrollmentJpaEntity enrollment) {
            return from(enrollment, Map.of());
        }

        public static EnrolledStudentResponse from(ClassEnrollmentJpaEntity enrollment, Map<UUID, String> userFullNames) {
            var student = enrollment.getStudent();
            return new EnrolledStudentResponse(student.getId(), enrollment.getId(), student.getFullName(), student.getEmail(),
                    "*** *** ** **", enrollment.getStatus(), enrollment.getRegistrationFee(),
                    enrollment.getPaymentPlan(), enrollment.getInstallmentCount(), enrollment.getFirstPaymentDate(),
                    enrollment.getPaymentStatus(), enrollment.getExpectedPaymentDate(), enrollment.getNote(),
                    enrollment.getPayments().stream().map(payment -> EnrollmentPaymentResponse.from(payment, userFullNames)).toList(),
                    enrollment.getVersion());
        }
    }

    public record EnrollmentPaymentResponse(UUID id, int installmentNumber, int installmentTotal,
            BigDecimal amount, LocalDate dueDate, PaymentStatus status, LocalDate paidAt,
            PaymentMethod paymentMethod, UUID receivedByUserId, String receivedByFullName, long version) {
        public static EnrollmentPaymentResponse from(EnrollmentPaymentJpaEntity payment) {
            return from(payment, Map.of());
        }

        public static EnrollmentPaymentResponse from(EnrollmentPaymentJpaEntity payment, Map<UUID, String> userFullNames) {
            return new EnrollmentPaymentResponse(payment.getId(), payment.getInstallmentNumber(),
                    payment.getInstallmentTotal(), payment.getAmount(), payment.getDueDate(), payment.getStatus(),
                    payment.getPaidAt(), payment.getPaymentMethod(), payment.getReceivedByUserId(),
                    payment.getReceivedByUserId() == null ? null : userFullNames.get(payment.getReceivedByUserId()),
                    payment.getVersion());
        }
    }
}
