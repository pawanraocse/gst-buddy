package com.learning.backendservice.domain.gstr3b;

import com.learning.backendservice.domain.gstr1.ReliefWindowSnapshot;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Immutable input for the GSTR-3B Late Fee audit rule.
 *
 * <p><b>Legal basis:</b> Section 47(2), CGST Act 2017.
 *
 * @param gstin            15-character validated GSTIN
 * @param filingDate       actual date the GSTR-3B was filed (from portal JSON/PDF)
 * @param taxPeriod        tax period this return covers (month or quarter)
 * @param financialYear    GST financial year string, e.g. "2024-25"
 * @param isNilReturn      true if net tax payable is nil
 * @param isQrmp           true if taxpayer is a QRMP (quarterly) filer
 * @param stateCode        first two characters of GSTIN — determines 22nd vs 24th due date
 * @param reliefWindow     pre-resolved CBIC amnesty window, or {@code null} if none applies
 */
public record Gstr3bLateFeeInput(
        String               gstin,
        LocalDate            filingDate,
        YearMonth            taxPeriod,
        String               financialYear,
        boolean              isNilReturn,
        boolean              isQrmp,
        String               stateCode,
        ReliefWindowSnapshot reliefWindow
) {}
