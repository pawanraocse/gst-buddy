package com.learning.backendservice.domain.risk;

import java.math.BigDecimal;
import java.util.List;

public record SupplierRiskResult(
        int totalSuppliersChecked,
        int highRiskCount,
        BigDecimal highRiskExposure,
        List<SupplierScore> supplierScores
) {}
