package com.cmsBackend.ws.training.api.model;

import com.cmsBackend.ws.training.domain.ClassStatus;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.UUID;

public record UpdateClassRequest(
        @NotBlank @Size(max = 160) String name,
        @NotNull UUID courseId,
        @NotBlank @Size(max = 120) String instructorName,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @Min(1) @Max(50) int capacity,
        @NotNull ClassStatus status,
        @Min(0) long version) {}
