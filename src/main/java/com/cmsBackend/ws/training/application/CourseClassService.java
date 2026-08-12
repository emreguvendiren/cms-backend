package com.cmsBackend.ws.training.application;

import com.cmsBackend.ws.training.api.model.ClassResponse;
import com.cmsBackend.ws.training.api.model.CreateClassRequest;
import com.cmsBackend.ws.training.api.model.CreateClassEnrollmentRequest;
import com.cmsBackend.ws.training.api.model.UpdateClassRequest;
import com.cmsBackend.ws.training.api.model.UpdateClassEnrollmentRequest;
import com.cmsBackend.ws.training.api.model.ReceiveEnrollmentPaymentRequest;
import com.cmsBackend.ws.training.api.model.ClassDetailResponse;
import com.cmsBackend.ws.training.api.model.PageResponse;
import com.cmsBackend.ws.training.domain.ClassStatus;
import com.cmsBackend.ws.training.domain.CourseStatus;
import com.cmsBackend.ws.training.domain.EnrollmentStatus;
import com.cmsBackend.ws.training.domain.PaymentPlanType;
import com.cmsBackend.ws.training.domain.PaymentStatus;
import com.cmsBackend.ws.student.domain.StudentStatus;
import com.cmsBackend.ws.common.security.audit.SecurityAuditService;
import com.cmsBackend.ws.training.infrastructure.persistence.ClassEnrollmentJpaEntity;
import com.cmsBackend.ws.training.infrastructure.persistence.EnrollmentPaymentJpaEntity;
import com.cmsBackend.ws.training.infrastructure.persistence.CourseClassJpaEntity;
import com.cmsBackend.ws.training.infrastructure.persistence.CourseClassRepository;
import com.cmsBackend.ws.training.infrastructure.persistence.CourseRepository;
import com.cmsBackend.ws.training.infrastructure.persistence.ClassEnrollmentRepository;
import com.cmsBackend.ws.training.infrastructure.persistence.StudentRepository;
import com.cmsBackend.ws.user.infrastructure.persistence.SpringDataUserAccountRepository;
import com.cmsBackend.ws.user.infrastructure.persistence.UserAccountJpaEntity;
import java.util.UUID;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseClassService {
    private final CourseClassRepository classes;
    private final CourseRepository courses;
    private final ClassEnrollmentRepository enrollments;
    private final StudentRepository students;
    private final SpringDataUserAccountRepository users;
    private final SecurityAuditService audit;

    public CourseClassService(CourseClassRepository classes, CourseRepository courses,
            ClassEnrollmentRepository enrollments, StudentRepository students,
            SpringDataUserAccountRepository users, SecurityAuditService audit) {
        this.classes = classes;
        this.courses = courses;
        this.enrollments = enrollments;
        this.students = students;
        this.users = users;
        this.audit = audit;
    }

    @PreAuthorize("hasAuthority('class:read')")
    @Transactional(readOnly = true)
    public PageResponse<ClassResponse> list(String search, ClassStatus status, UUID courseId, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("name").ascending().and(Sort.by("id").ascending()));
        return PageResponse.from(
                classes.search(normalizeSearch(search), status, courseId, pageable).map(ClassResponse::from));
    }

    @PreAuthorize("hasAuthority('class:create')")
    @Transactional
    public ClassResponse create(CreateClassRequest request) {
        validateClassSchedule(request.startDate(), request.endDate(), request.startTime(), request.endTime());
        UUID id = UUID.randomUUID();
        String code = "SNF-" + id.toString().substring(0, 8).toUpperCase(java.util.Locale.ROOT);
        var course = courses.findById(request.courseId()).orElseThrow(TrainingNotFoundException::new);
        if (course.getStatus() == CourseStatus.ARCHIVED) throw new TrainingConflictException();
        var courseClass = new CourseClassJpaEntity(
                id, code, request.name().trim(), course, request.instructorName().trim(),
                request.startDate(), request.endDate(), request.startTime(), request.endTime(),
                request.capacity(), request.status());
        return ClassResponse.from(classes.save(courseClass));
    }

    @PreAuthorize("hasAuthority('class:read')")
    @Transactional(readOnly = true)
    public ClassDetailResponse detail(UUID id) {
        var courseClass = classes.findById(id).orElseThrow(TrainingNotFoundException::new);
        var students = enrollments.findByCourseClassIdOrderByStudentFullNameAsc(id).stream()
                .toList();
        var userFullNames = userFullNames(students.stream()
                .flatMap(enrollment -> enrollment.getPayments().stream())
                .map(payment -> payment.getReceivedByUserId()).toList());
        var responses = students.stream()
                .map(enrollment -> ClassDetailResponse.EnrolledStudentResponse.from(enrollment, userFullNames)).toList();
        return new ClassDetailResponse(ClassResponse.from(courseClass), responses);
    }

    @PreAuthorize("hasAuthority('class:enrollment:create')")
    @Transactional
    public ClassDetailResponse.EnrolledStudentResponse enroll(
            UUID classId, CreateClassEnrollmentRequest request, UUID actorId) {
        validatePaymentPlan(request);
        var courseClass = classes.findForEnrollmentById(classId).orElseThrow(TrainingNotFoundException::new);
        var student = students.findByIdAndDeletedAtIsNull(request.studentId())
                .orElseThrow(TrainingNotFoundException::new);
        if (student.getStatus() == StudentStatus.INACTIVE) throw new TrainingConflictException();
        if (enrollments.existsByCourseClassIdAndStudentId(classId, student.getId())) {
            throw new TrainingConflictException();
        }
        long activeEnrollmentCount = enrollments.countByCourseClassIdAndStatusNot(classId, EnrollmentStatus.CANCELLED);
        if (activeEnrollmentCount >= courseClass.getCapacity()) throw new TrainingConflictException();

        student.activateForEnrollment(courseClass.getCourse().getName());
        var enrollment = new ClassEnrollmentJpaEntity(
                UUID.randomUUID(), courseClass, student, EnrollmentStatus.ACTIVE,
                request.registrationFee(), request.paymentPlan(),
                isScheduledPaymentPlan(request.paymentPlan()) ? request.installmentCount() : null,
                isScheduledPaymentPlan(request.paymentPlan()) ? request.firstPaymentDate() : null,
                request.paymentStatus(),
                request.paymentPlan() == PaymentPlanType.CASH && request.paymentStatus() == PaymentStatus.PENDING
                        ? request.expectedPaymentDate() : null,
                normalizeNote(request.note()));
        enrollment.replacePayments(createPaymentSchedule(enrollment, request.registrationFee(), request.paymentPlan(),
                request.installmentCount(), request.firstPaymentDate(), request.paymentStatus(),
                request.expectedPaymentDate()));
        var saved = enrollments.saveAndFlush(enrollment);
        audit.classEnrollmentCreated(actorId, classId, student.getId());
        return ClassDetailResponse.EnrolledStudentResponse.from(saved, paymentUserFullNames(saved));
    }

    @PreAuthorize("hasAuthority('class:enrollment:update')")
    @Transactional
    public ClassDetailResponse.EnrolledStudentResponse updateEnrollment(
            UUID classId, UUID enrollmentId, UpdateClassEnrollmentRequest request, UUID actorId) {
        validatePaymentPlan(request);
        var enrollment = enrollments.findByIdAndCourseClassId(enrollmentId, classId)
                .orElseThrow(TrainingNotFoundException::new);
        if (enrollment.getVersion() != request.version()) throw new TrainingConflictException();
        boolean financialChanged = !Objects.equals(enrollment.getRegistrationFee(), request.registrationFee())
                || enrollment.getPaymentPlan() != request.paymentPlan()
                || !Objects.equals(enrollment.getInstallmentCount(), request.installmentCount())
                || !Objects.equals(enrollment.getFirstPaymentDate(), request.firstPaymentDate())
                || enrollment.getPaymentStatus() != request.paymentStatus()
                || !Objects.equals(enrollment.getExpectedPaymentDate(), request.expectedPaymentDate());
        if (financialChanged && enrollment.getPayments().stream()
                .anyMatch(payment -> payment.getStatus() == PaymentStatus.COMPLETED)) {
            throw new TrainingConflictException();
        }
        enrollment.updatePayment(request.registrationFee(), request.paymentPlan(),
                isScheduledPaymentPlan(request.paymentPlan()) ? request.installmentCount() : null,
                isScheduledPaymentPlan(request.paymentPlan()) ? request.firstPaymentDate() : null,
                request.paymentStatus(),
                request.paymentPlan() == PaymentPlanType.CASH && request.paymentStatus() == PaymentStatus.PENDING
                        ? request.expectedPaymentDate() : null,
                normalizeNote(request.note()));
        if (financialChanged) {
            enrollment.replacePayments(createPaymentSchedule(enrollment, request.registrationFee(), request.paymentPlan(),
                    request.installmentCount(), request.firstPaymentDate(), request.paymentStatus(),
                    request.expectedPaymentDate()));
        }
        var saved = enrollments.saveAndFlush(enrollment);
        audit.classEnrollmentChanged("update", actorId, classId, enrollment.getStudent().getId());
        return ClassDetailResponse.EnrolledStudentResponse.from(saved, paymentUserFullNames(saved));
    }

    @PreAuthorize("hasAuthority('class:enrollment:update')")
    @Transactional
    public ClassDetailResponse.EnrolledStudentResponse receivePayment(UUID classId, UUID enrollmentId,
            UUID paymentId, ReceiveEnrollmentPaymentRequest request, UUID actorId) {
        var enrollment = enrollments.findByIdAndCourseClassId(enrollmentId, classId)
                .orElseThrow(TrainingNotFoundException::new);
        var payment = enrollment.getPayments().stream().filter(item -> item.getId().equals(paymentId))
                .findFirst().orElseThrow(TrainingNotFoundException::new);
        if (payment.getVersion() != request.version() || payment.getStatus() == PaymentStatus.COMPLETED) {
            throw new TrainingConflictException();
        }
        payment.markReceived(request.paidAt(), request.paymentMethod(), actorId);
        enrollment.refreshPaymentStatus();
        var saved = enrollments.saveAndFlush(enrollment);
        audit.classEnrollmentChanged("payment_receive", actorId, classId, enrollment.getStudent().getId());
        return ClassDetailResponse.EnrolledStudentResponse.from(saved, paymentUserFullNames(saved));
    }

    @PreAuthorize("hasAuthority('class:enrollment:delete')")
    @Transactional
    public void deleteEnrollment(UUID classId, UUID enrollmentId, UUID actorId) {
        var enrollment = enrollments.findByIdAndCourseClassId(enrollmentId, classId)
                .orElseThrow(TrainingNotFoundException::new);
        var studentId = enrollment.getStudent().getId();
        enrollments.delete(enrollment);
        enrollments.flush();
        audit.classEnrollmentChanged("delete", actorId, classId, studentId);
    }

    @PreAuthorize("hasAuthority('class:update')")
    @Transactional
    public ClassResponse update(UUID id, UpdateClassRequest request) {
        validateClassSchedule(request.startDate(), request.endDate(), request.startTime(), request.endTime());
        var courseClass = classes.findById(id).orElseThrow(TrainingNotFoundException::new);
        if (courseClass.getVersion() != request.version()) throw new TrainingConflictException();
        var course = courses.findById(request.courseId()).orElseThrow(TrainingNotFoundException::new);
        if (course.getStatus() == CourseStatus.ARCHIVED) throw new TrainingConflictException();
        if (request.capacity() < courseClass.getEnrolledCount()) throw new TrainingConflictException();
        courseClass.update(request.name().trim(), course, request.instructorName().trim(), request.startDate(),
                request.endDate(), request.startTime(), request.endTime(), request.capacity(), request.status());
        return ClassResponse.from(classes.saveAndFlush(courseClass));
    }

    @PreAuthorize("hasAuthority('class:delete')")
    @Transactional
    public void delete(UUID id) {
        if (!classes.existsById(id)) throw new TrainingNotFoundException();
        if (enrollments.existsByCourseClassId(id)) throw new TrainingConflictException();
        classes.deleteById(id);
    }

    private String normalizeSearch(String search) {
        return search == null ? "" : search.trim();
    }

    private Map<UUID, String> paymentUserFullNames(ClassEnrollmentJpaEntity enrollment) {
        return userFullNames(enrollment.getPayments().stream().map(payment -> payment.getReceivedByUserId()).toList());
    }

    private Map<UUID, String> userFullNames(Collection<UUID> ids) {
        var userIds = ids.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (userIds.isEmpty()) return Map.of();
        return users.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserAccountJpaEntity::getId, UserAccountJpaEntity::getFullName));
    }

    private void validateClassSchedule(LocalDate startDate, LocalDate endDate,
            java.time.LocalTime startTime, java.time.LocalTime endTime) {
        if (endDate.isBefore(startDate)) throw new IllegalArgumentException("Invalid date range.");
        if (!endTime.isAfter(startTime)) throw new IllegalArgumentException("Invalid time range.");
    }

    private void validatePaymentPlan(CreateClassEnrollmentRequest request) {
        if (isScheduledPaymentPlan(request.paymentPlan())
                && (request.installmentCount() == null || request.firstPaymentDate() == null)) {
            throw new IllegalArgumentException("Installment count and first payment date are required for scheduled payment plans.");
        }
        if (isScheduledPaymentPlan(request.paymentPlan()) && request.paymentStatus() == PaymentStatus.COMPLETED) {
            throw new IllegalArgumentException("Scheduled payment plans cannot start as completed.");
        }
        if (request.paymentPlan() == PaymentPlanType.CASH
                && (request.installmentCount() != null || request.firstPaymentDate() != null)) {
            throw new IllegalArgumentException("Cash payment cannot contain installment details.");
        }
        boolean pendingCash = request.paymentPlan() == PaymentPlanType.CASH
                && request.paymentStatus() == PaymentStatus.PENDING;
        if (pendingCash && request.expectedPaymentDate() == null) {
            throw new IllegalArgumentException("Expected payment date is required for pending cash payments.");
        }
        if (!pendingCash && request.expectedPaymentDate() != null) {
            throw new IllegalArgumentException("Expected payment date is only valid for pending cash payments.");
        }
    }

    private void validatePaymentPlan(UpdateClassEnrollmentRequest request) {
        if (isScheduledPaymentPlan(request.paymentPlan())
                && (request.installmentCount() == null || request.firstPaymentDate() == null)) {
            throw new IllegalArgumentException("Installment count and first payment date are required for scheduled payment plans.");
        }
        if (isScheduledPaymentPlan(request.paymentPlan()) && request.paymentStatus() == PaymentStatus.COMPLETED) {
            throw new IllegalArgumentException("Scheduled payment plans cannot start as completed.");
        }
        if (request.paymentPlan() == PaymentPlanType.CASH
                && (request.installmentCount() != null || request.firstPaymentDate() != null)) {
            throw new IllegalArgumentException("Cash payment cannot contain installment details.");
        }
        boolean pendingCash = request.paymentPlan() == PaymentPlanType.CASH
                && request.paymentStatus() == PaymentStatus.PENDING;
        if (pendingCash && request.expectedPaymentDate() == null) {
            throw new IllegalArgumentException("Expected payment date is required for pending cash payments.");
        }
        if (!pendingCash && request.expectedPaymentDate() != null) {
            throw new IllegalArgumentException("Expected payment date is only valid for pending cash payments.");
        }
    }

    private String normalizeNote(String note) {
        return note == null || note.isBlank() ? null : note.trim();
    }

    private java.util.List<EnrollmentPaymentJpaEntity> createPaymentSchedule(
            ClassEnrollmentJpaEntity enrollment, BigDecimal totalAmount, PaymentPlanType plan,
            Integer installmentCount, LocalDate firstPaymentDate, PaymentStatus status,
            LocalDate expectedPaymentDate) {
        int count = isScheduledPaymentPlan(plan) ? installmentCount : 1;
        BigDecimal regularAmount = totalAmount.divide(BigDecimal.valueOf(count), 2, RoundingMode.DOWN);
        BigDecimal allocated = regularAmount.multiply(BigDecimal.valueOf(count - 1L));
        var result = new ArrayList<EnrollmentPaymentJpaEntity>(count);
        for (int index = 0; index < count; index++) {
            BigDecimal amount = index == count - 1 ? totalAmount.subtract(allocated) : regularAmount;
            LocalDate dueDate = isScheduledPaymentPlan(plan)
                    ? firstPaymentDate.plusMonths(index) : expectedPaymentDate;
            LocalDate paidAt = status == PaymentStatus.COMPLETED ? LocalDate.now() : null;
            result.add(new EnrollmentPaymentJpaEntity(UUID.randomUUID(), enrollment, index + 1, count,
                    amount, dueDate, status, paidAt));
        }
        return result;
    }

    private boolean isScheduledPaymentPlan(PaymentPlanType plan) {
        return plan == PaymentPlanType.INSTALLMENT || plan == PaymentPlanType.PROMISSORY_NOTE;
    }
}
