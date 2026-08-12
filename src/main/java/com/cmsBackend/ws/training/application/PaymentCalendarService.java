package com.cmsBackend.ws.training.application;

import com.cmsBackend.ws.training.api.model.PaymentCalendarResponse;
import com.cmsBackend.ws.training.domain.EnrollmentStatus;
import com.cmsBackend.ws.training.domain.PaymentPlanType;
import com.cmsBackend.ws.training.domain.PaymentStatus;
import com.cmsBackend.ws.training.infrastructure.persistence.EnrollmentPaymentRepository;
import java.time.YearMonth;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentCalendarService {
    private final EnrollmentPaymentRepository payments;

    public PaymentCalendarService(EnrollmentPaymentRepository payments) {
        this.payments = payments;
    }

    @PreAuthorize("hasAuthority('class:enrollment:update')")
    @Transactional(readOnly = true)
    public PaymentCalendarResponse getMonth(YearMonth month) {
        var items = payments.findPaymentCalendar(month.atDay(1), month.atEndOfMonth(), PaymentStatus.COMPLETED,
                PaymentStatus.PENDING, EnrollmentStatus.CANCELLED,
                List.of(PaymentPlanType.INSTALLMENT, PaymentPlanType.PROMISSORY_NOTE))
                .stream()
                .map(PaymentCalendarResponse.PaymentCalendarItemResponse::from)
                .toList();
        return new PaymentCalendarResponse(month.toString(), items);
    }
}
