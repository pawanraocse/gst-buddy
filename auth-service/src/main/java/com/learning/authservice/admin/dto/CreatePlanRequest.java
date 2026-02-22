package com.learning.authservice.admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Request body for creating a new pricing plan.
 */
public record CreatePlanRequest(
        @NotBlank String name,
        @NotBlank String displayName,
        @NotNull @Min(0) BigDecimal priceInr,
        @NotNull @Min(1) Integer credits,
        boolean isTrial,
        String description,
        Integer validityDays,
        Integer sortOrder) {
}
