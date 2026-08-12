package com.cmsBackend.ws.training.infrastructure.persistence;

import com.cmsBackend.ws.training.domain.PaymentMethod;
import com.cmsBackend.ws.training.domain.PaymentStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "enrollment_payments",
        uniqueConstraints = @UniqueConstraint(name = "uk_enrollment_payment_sequence",
                columnNames = {"enrollment_id", "installment_number"}),
        indexes = @Index(name = "idx_enrollment_payments_enrollment", columnList = "enrollment_id"))
public class EnrollmentPaymentJpaEntity {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private ClassEnrollmentJpaEntity enrollment;
    @Column(name = "installment_number", nullable = false) private int installmentNumber;
    @Column(name = "installment_total", nullable = false) private int installmentTotal;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    @Column(name = "due_date") private LocalDate dueDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private PaymentStatus status;
    @Column(name = "paid_at") private LocalDate paidAt;
    @Enumerated(EnumType.STRING) @Column(name = "payment_method", length = 30) private PaymentMethod paymentMethod;
    @Column(name = "received_by_user_id") private UUID receivedByUserId;
    @Version private long version;

    protected EnrollmentPaymentJpaEntity() {}

    public EnrollmentPaymentJpaEntity(UUID id, ClassEnrollmentJpaEntity enrollment, int installmentNumber,
            int installmentTotal, BigDecimal amount, LocalDate dueDate, PaymentStatus status, LocalDate paidAt) {
        this.id = id; this.enrollment = enrollment; this.installmentNumber = installmentNumber;
        this.installmentTotal = installmentTotal; this.amount = amount; this.dueDate = dueDate;
        this.status = status; this.paidAt = paidAt;
    }

    public UUID getId() { return id; }
    public int getInstallmentNumber() { return installmentNumber; }
    public int getInstallmentTotal() { return installmentTotal; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getDueDate() { return dueDate; }
    public PaymentStatus getStatus() { return status; }
    public LocalDate getPaidAt() { return paidAt; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public UUID getReceivedByUserId() { return receivedByUserId; }
    public long getVersion() { return version; }

    public void markReceived(LocalDate receivedAt, PaymentMethod method) {
        markReceived(receivedAt, method, null);
    }

    public void markReceived(LocalDate receivedAt, PaymentMethod method, UUID actorId) {
        if (status == PaymentStatus.COMPLETED) throw new IllegalStateException("Payment is already completed.");
        status = PaymentStatus.COMPLETED;
        paidAt = receivedAt;
        paymentMethod = method;
        receivedByUserId = actorId;
    }
}
