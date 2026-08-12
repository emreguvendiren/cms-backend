package com.cmsBackend.ws.training.api.model;

import com.cmsBackend.ws.training.domain.ClassStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record CreateClassRequest(
        @NotBlank @Size(max = 160) String name,
        @NotNull UUID courseId,
        @NotBlank @Size(max = 120) String instructorName,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @Min(1) @Max(50) int capacity,
        @NotNull ClassStatus status) {}
