package com.cmsBackend.ws.student.api.model;

import com.cmsBackend.ws.training.api.model.ClassDetailResponse;
import com.cmsBackend.ws.training.domain.ClassStatus;
import com.cmsBackend.ws.training.domain.EnrollmentStatus;
import com.cmsBackend.ws.training.domain.PaymentPlanType;
import com.cmsBackend.ws.training.domain.PaymentStatus;
import com.cmsBackend.ws.training.infrastructure.persistence.ClassEnrollmentJpaEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record StudentEnrollmentResponse(UUID classId, String classCode, String className, UUID courseId,
        String courseCode, String courseName, String instructorName, LocalDate startDate, LocalDate endDate,
        ClassStatus classStatus, UUID enrollmentId, EnrollmentStatus enrollmentStatus, BigDecimal registrationFee,
        PaymentPlanType paymentPlan, Integer installmentCount, LocalDate firstPaymentDate,
        PaymentStatus paymentStatus, LocalDate expectedPaymentDate, String note,
        List<ClassDetailResponse.EnrollmentPaymentResponse> payments, long version) {
    public static StudentEnrollmentResponse from(ClassEnrollmentJpaEntity enrollment) {
        var courseClass = enrollment.getCourseClass();
        var course = courseClass.getCourse();
        return new StudentEnrollmentResponse(courseClass.getId(), courseClass.getCode(), courseClass.getName(),
                course.getId(), course.getCode(), course.getName(), courseClass.getInstructorName(),
                courseClass.getStartDate(), courseClass.getEndDate(), courseClass.getStatus(), enrollment.getId(),
                enrollment.getStatus(), enrollment.getRegistrationFee(), enrollment.getPaymentPlan(),
                enrollment.getInstallmentCount(), enrollment.getFirstPaymentDate(), enrollment.getPaymentStatus(),
                enrollment.getExpectedPaymentDate(), enrollment.getNote(),
                enrollment.getPayments().stream().map(ClassDetailResponse.EnrollmentPaymentResponse::from).toList(),
                enrollment.getVersion());
    }
}
