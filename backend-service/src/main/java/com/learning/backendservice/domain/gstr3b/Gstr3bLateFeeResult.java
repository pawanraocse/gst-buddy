package com.learning.backendservice.domain.gstr3b;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Computed result of the GSTR-3B late fee calculation.
 * All monetary amounts are BigDecimal with scale=2.
 *
 * @param dueDate           statutory due date for this return (state-aware for QRMP)
 * @param filingDate        actual filing date from the document
 * @param delayDays         days of delay (0 if filed on time)
 * @param cgstFee           late fee under CGST head (Section 47(2))
 * @param sgstFee           late fee under SGST head (Section 47(2))
 * @param totalFee          cgstFee + sgstFee
 * @param reliefApplied     true if a CBIC notification reduced the fee
 * @param appliedNotification notification number of the relief, or null
 */
public record Gstr3bLateFeeResult(
        LocalDate  dueDate,
        LocalDate  filingDate,
        int        delayDays,
        BigDecimal cgstFee,
        BigDecimal sgstFee,
        BigDecimal totalFee,
        boolean    reliefApplied,
        String     appliedNotification
) {
    /** Factory for on-time filings. */
    public static Gstr3bLateFeeResult onTime(LocalDate dueDate, LocalDate filingDate) {
        return new Gstr3bLateFeeResult(
                dueDate, filingDate, 0,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                false, null);
    }
}
