package com.cmsBackend.ws.training.api;

import com.cmsBackend.ws.training.api.model.PaymentCalendarResponse;
import com.cmsBackend.ws.training.application.PaymentCalendarService;
import jakarta.validation.constraints.Pattern;
import java.time.YearMonth;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
public class PaymentCalendarController {
    private final PaymentCalendarService service;

    public PaymentCalendarController(PaymentCalendarService service) {
        this.service = service;
    }

    @GetMapping("/api/payment-calendar")
    public PaymentCalendarResponse paymentCalendar(
            @RequestParam @Pattern(regexp = "\\d{4}-\\d{2}") String month) {
        return service.getMonth(YearMonth.parse(month));
    }
}
