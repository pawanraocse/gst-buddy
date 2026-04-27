package com.learning.backendservice.domain.itc;

import com.learning.backendservice.domain.shared.PurchaseRegisterRow;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public record ItcRecoInput(
        String gstin,
        YearMonth taxPeriod,
        String financialYear,
        List<PurchaseRegisterRow> purchaseRegisterRows,
        List<PurchaseRegisterRow> gstr2bRows,
        BigDecimal reconToleranceAmount
) {}
