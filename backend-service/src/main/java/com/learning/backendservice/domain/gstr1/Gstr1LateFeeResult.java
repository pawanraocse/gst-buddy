package com.learning.backendservice.domain.gstr1;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Immutable result of the GSTR-1 Late Fee calculation.
 *
 * <p>All monetary amounts use {@code BigDecimal} with scale=2, RoundingMode.HALF_UP
 * per the GST Expert skill. Amounts are always split per tax head (CGST/SGST) —
 * never combined — to satisfy Section 47(1), CGST Act 2017 legal reporting requirements.
 *
 * @param dueDate             statutory due date for this filing (11th Monthly / 13th QRMP)
 * @param arnDate             actual filing date extracted from the document
 * @param delayDays           calendar days late (0 = on time or early; always non-negative)
 * @param cgstFee             final CGST late fee after applying caps/relief (₹)
 * @param sgstFee             final SGST late fee after applying caps/relief (₹)
 * @param totalFee            sum of cgstFee + sgstFee (convenience field)
 * @param reliefApplied       true if a CBIC amnesty/waiver notification reduced the fee
 * @param appliedNotification notification reference if relief applied, otherwise null
 */
public record Gstr1LateFeeResult(
        LocalDate  dueDate,
        LocalDate  arnDate,
        int        delayDays,
        BigDecimal cgstFee,
        BigDecimal sgstFee,
        BigDecimal totalFee,
        boolean    reliefApplied,
        String     appliedNotification
) {
    /** Convenience factory — filed on time, zero fee. */
    public static Gstr1LateFeeResult onTime(LocalDate dueDate, LocalDate arnDate) {
        return new Gstr1LateFeeResult(
                dueDate, arnDate, 0,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                false, null
        );
    }
}
