package com.cmsBackend.ws.training.api.model;

import com.cmsBackend.ws.training.domain.CourseStatus;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record UpdateCourseRequest(
        @NotBlank @Size(max = 160) String name,
        @NotBlank @Size(max = 80) String category,
        @Min(1) @Max(500) int durationHours,
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal listPrice,
        @NotNull CourseStatus status,
        @Min(0) long version) {}
