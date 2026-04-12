package com.learning.authservice.credit.dto;

import lombok.Builder;

import java.math.BigDecimal;

/**
 * Response DTO for pricing plan display.
 */
@Builder
public record PlanDto(
        Long id,
        String name,
        String displayName,
        BigDecimal priceInr,
        BigDecimal salePriceInr,
        boolean isSaleActive,
        int credits,
        boolean isTrial,
        boolean isActive,
        String description,
        Integer validityDays,
        int sortOrder) {
}
