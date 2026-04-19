package com.learning.backendservice.domain.gstr3b;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

public record Gstr3bInterestInput(
        String gstin,
        LocalDate filingDate,
        YearMonth taxPeriod,
        String financialYear,
        boolean isQrmp,
        String stateCode,
        BigDecimal cgstCashPaid,
        BigDecimal sgstCashPaid,
        BigDecimal igstCashPaid
) {}
