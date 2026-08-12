package com.cmsBackend.ws.training.infrastructure.persistence;

import com.cmsBackend.ws.training.domain.EnrollmentStatus;
import com.cmsBackend.ws.training.domain.PaymentPlanType;
import com.cmsBackend.ws.training.domain.PaymentStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "class_enrollments", uniqueConstraints = @UniqueConstraint(name = "uk_class_enrollment_student", columnNames = {"class_id", "student_id"}), indexes = {@Index(name="idx_class_enrollments_class", columnList="class_id"), @Index(name="idx_class_enrollments_student", columnList="student_id")})
public class ClassEnrollmentJpaEntity {
    @Id private UUID id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="class_id", nullable=false) private CourseClassJpaEntity courseClass;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="student_id", nullable=false) private StudentJpaEntity student;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private EnrollmentStatus status;
    @Column(name="registration_fee", nullable=false, precision=12, scale=2, columnDefinition="numeric(12,2) default 0") private BigDecimal registrationFee;
    @Enumerated(EnumType.STRING) @Column(name="payment_plan", nullable=false, length=20, columnDefinition="varchar(20) default 'CASH'") private PaymentPlanType paymentPlan;
    @Column(name="installment_count") private Integer installmentCount;
    @Column(name="first_payment_date") private LocalDate firstPaymentDate;
    @Enumerated(EnumType.STRING) @Column(name="payment_status", nullable=false, length=20, columnDefinition="varchar(20) default 'PENDING'") private PaymentStatus paymentStatus;
    @Column(name="expected_payment_date") private LocalDate expectedPaymentDate;
    @Column(length=1000) private String note;
    @OneToMany(mappedBy = "enrollment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("installmentNumber ASC")
    private List<EnrollmentPaymentJpaEntity> payments = new ArrayList<>();
    @Version private long version;
    protected ClassEnrollmentJpaEntity() {}
    public ClassEnrollmentJpaEntity(UUID id, CourseClassJpaEntity courseClass, StudentJpaEntity student, EnrollmentStatus status){
        this(id, courseClass, student, status, BigDecimal.ZERO, PaymentPlanType.CASH, null, null, PaymentStatus.PENDING, null, null);
    }
    public ClassEnrollmentJpaEntity(UUID id, CourseClassJpaEntity courseClass, StudentJpaEntity student,
            EnrollmentStatus status, BigDecimal registrationFee, PaymentPlanType paymentPlan,
            Integer installmentCount, LocalDate firstPaymentDate, PaymentStatus paymentStatus,
            LocalDate expectedPaymentDate, String note) {
        this.id=id; this.courseClass=courseClass; this.student=student; this.status=status;
        this.registrationFee=registrationFee; this.paymentPlan=paymentPlan; this.installmentCount=installmentCount;
        this.firstPaymentDate=firstPaymentDate; this.paymentStatus=paymentStatus;
        this.expectedPaymentDate=expectedPaymentDate; this.note=note;
    }
    public UUID getId(){return id;} public CourseClassJpaEntity getCourseClass(){return courseClass;} public StudentJpaEntity getStudent(){return student;} public EnrollmentStatus getStatus(){return status;}
    public BigDecimal getRegistrationFee(){return registrationFee;} public PaymentPlanType getPaymentPlan(){return paymentPlan;}
    public Integer getInstallmentCount(){return installmentCount;} public LocalDate getFirstPaymentDate(){return firstPaymentDate;}
    public PaymentStatus getPaymentStatus(){return paymentStatus;} public LocalDate getExpectedPaymentDate(){return expectedPaymentDate;}
    public String getNote(){return note;} public long getVersion(){return version;}
    public List<EnrollmentPaymentJpaEntity> getPayments(){return Collections.unmodifiableList(payments);}
    public void replacePayments(List<EnrollmentPaymentJpaEntity> nextPayments) {
        payments.clear(); payments.addAll(nextPayments);
    }
    public void updatePayment(BigDecimal registrationFee, PaymentPlanType paymentPlan, Integer installmentCount,
            LocalDate firstPaymentDate, PaymentStatus paymentStatus, LocalDate expectedPaymentDate, String note) {
        this.registrationFee=registrationFee; this.paymentPlan=paymentPlan; this.installmentCount=installmentCount;
        this.firstPaymentDate=firstPaymentDate; this.paymentStatus=paymentStatus;
        this.expectedPaymentDate=expectedPaymentDate; this.note=note;
    }
    public void refreshPaymentStatus() {
        paymentStatus = !payments.isEmpty() && payments.stream()
                .allMatch(payment -> payment.getStatus() == PaymentStatus.COMPLETED)
                ? PaymentStatus.COMPLETED : PaymentStatus.PENDING;
    }
}
