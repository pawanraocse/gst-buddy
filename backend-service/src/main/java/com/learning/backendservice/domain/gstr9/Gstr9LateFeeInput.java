package com.learning.backendservice.domain.gstr9;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Gstr9LateFeeInput(
        String gstin,
        LocalDate filingDate,
        String financialYear,
        BigDecimal aggregateTurnover
) {}
