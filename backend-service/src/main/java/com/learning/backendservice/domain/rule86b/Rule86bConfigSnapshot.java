package com.learning.backendservice.domain.rule86b;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Immutable snapshot of Rule 86B configuration loaded into SharedResources.
 */
public record Rule86bConfigSnapshot(
        BigDecimal turnoverThreshold,
        BigDecimal cashPercentFloor,
        LocalDate effectiveFrom
) {
    public static Rule86bConfigSnapshot defaults() {
        return new Rule86bConfigSnapshot(
                new BigDecimal("5000000.00"),
                new BigDecimal("0.0100"),
                LocalDate.of(2021, 1, 1)
        );
    }
}
