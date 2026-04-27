package com.learning.backendservice.domain.itc;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public record ItcRecoResult(
        YearMonth taxPeriod,
        List<ItcMismatch> mismatches,
        BigDecimal totalItcAtRisk,
        int totalMatchedInvoices,
        int totalBooksInvoices,
        int total2bInvoices,
        String narrative
) {}
