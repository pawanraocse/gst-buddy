package com.learning.backendservice.domain.risk;

import java.math.BigDecimal;

public record SupplierScore(
        String gstin,
        String name,
        String status, // ACTIVE, CANCELLED
        int riskScore, // 0-100
        String riskCategory, // LOW, MEDIUM, HIGH, CRITICAL
        BigDecimal totalExposure
) {}
