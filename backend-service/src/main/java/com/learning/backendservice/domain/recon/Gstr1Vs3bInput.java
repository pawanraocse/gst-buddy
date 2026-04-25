package com.learning.backendservice.domain.recon;

import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * Immutable input for the GSTR-1 vs GSTR-3B reconciliation rule.
 *
 * <p>Built by {@link com.learning.backendservice.engine.resolvers.Gstr1Vs3bReconciliationInputResolver}
 * from the parsed documents and pre-loaded {@link com.learning.backendservice.engine.SharedResources}.
 *
 * @param gstin                 15-character GSTIN of the taxpayer
 * @param taxPeriod             the GST tax period being reconciled
 * @param financialYear         GST financial year string, e.g. "2024-25"
 * @param gstr1Liability        aggregated GSTR-1 outward liability totals
 * @param gstr3bTaxPayable      GSTR-3B Table 6.1 tax payable totals
 * @param reconToleranceAmount  maximum absolute delta (₹) to classify as MATCH
 * @param reconTolerancePercent maximum percentage delta to classify as MATCH (e.g. 0.0001 = 0.01%)
 */
public record Gstr1Vs3bInput(
        String             gstin,
        YearMonth          taxPeriod,
        String             financialYear,
        LiabilitySummary   gstr1Liability,
        TaxPaymentSummary  gstr3bTaxPayable,
        BigDecimal         reconToleranceAmount,
        BigDecimal         reconTolerancePercent
) {}
