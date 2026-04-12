package com.learning.authservice.admin.dto;

import java.math.BigDecimal;

/**
 * Request body for updating an existing pricing plan.
 * All fields are optional — only non-null values are applied.
 */
public record UpdatePlanRequest(
        String displayName,
        BigDecimal priceInr,
        BigDecimal salePriceInr,
        Boolean isSaleActive,
        Integer credits,
        Boolean isTrial,
        Boolean isActive,
        String description,
        Integer validityDays,
        Integer sortOrder) {
}
