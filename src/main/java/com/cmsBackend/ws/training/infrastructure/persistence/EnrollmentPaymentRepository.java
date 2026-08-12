package com.cmsBackend.ws.training.infrastructure.persistence;

import com.cmsBackend.ws.training.domain.EnrollmentStatus;
import com.cmsBackend.ws.training.domain.PaymentPlanType;
import com.cmsBackend.ws.training.domain.PaymentStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnrollmentPaymentRepository extends JpaRepository<EnrollmentPaymentJpaEntity, UUID> {
    @Query("""
            select payment.id as paymentId,
                   enrollment.id as enrollmentId,
                   courseClass.id as classId,
                   courseClass.code as classCode,
                   courseClass.name as className,
                   course.name as courseName,
                   student.id as studentId,
                   student.fullName as studentFullName,
                   enrollment.paymentPlan as paymentPlan,
                   payment.installmentNumber as installmentNumber,
                   payment.installmentTotal as installmentTotal,
                   payment.amount as amount,
                   payment.dueDate as dueDate,
                   payment.status as status,
                   payment.paidAt as paidAt,
                   payment.paymentMethod as paymentMethod
            from EnrollmentPaymentJpaEntity payment
            join payment.enrollment enrollment
            join enrollment.courseClass courseClass
            join courseClass.course course
            join enrollment.student student
            where enrollment.status <> :cancelled
              and (
                    (payment.status = :completed and payment.paidAt between :startDate and :endDate)
                    or
                    (payment.status = :pending and enrollment.paymentPlan in :expectedPlans
                        and payment.dueDate between :startDate and :endDate)
                  )
            order by coalesce(payment.paidAt, payment.dueDate) asc,
                     student.fullName asc,
                     payment.installmentNumber asc,
                     payment.id asc
            """)
    List<PaymentCalendarProjection> findPaymentCalendar(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("completed") PaymentStatus completed,
            @Param("pending") PaymentStatus pending,
            @Param("cancelled") EnrollmentStatus cancelled,
            @Param("expectedPlans") List<PaymentPlanType> expectedPlans);
}
