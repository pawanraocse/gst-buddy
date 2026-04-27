package com.learning.backendservice.domain.rcm;

import com.learning.backendservice.domain.shared.PurchaseRegisterRow;
import com.learning.backendservice.domain.recon.TaxPaymentSummary;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/**
 * Immutable input for the RCM reconciliation rule.
 */
public record RcmRecoInput(
        String gstin,
        YearMonth taxPeriod,
        String financialYear,
        List<PurchaseRegisterRow> purchaseRegisterRows,
        TaxPaymentSummary gstr3bRcmDeclared,
        BigDecimal reconToleranceAmount
) {}
