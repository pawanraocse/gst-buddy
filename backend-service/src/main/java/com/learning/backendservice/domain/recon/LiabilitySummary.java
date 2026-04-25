package com.learning.backendservice.domain.recon;

import java.math.BigDecimal;

/**
 * Aggregated outward liability totals extracted from GSTR-1.
 *
 * <p>All amounts represent the declared liability for a single tax period.
 * Populated from the {@code liability_summary} key in the GSTR-1 parser output.
 *
 * @param taxableValue  total taxable value across all outward supplies (₹)
 * @param igst          integrated GST liability (₹)
 * @param cgst          central GST liability (₹)
 * @param sgstUtgst     state / union territory GST liability (₹)
 * @param cess          cess liability (₹)
 */
public record LiabilitySummary(
        BigDecimal taxableValue,
        BigDecimal igst,
        BigDecimal cgst,
        BigDecimal sgstUtgst,
        BigDecimal cess
) {
    /**
     * Total tax declared in GSTR-1: IGST + CGST + SGST/UTGST + CESS.
     * Does NOT include taxable value.
     */
    public BigDecimal totalTax() {
        return igst.add(cgst).add(sgstUtgst).add(cess);
    }

    /** Convenience factory — all-zero summary (nil return). */
    public static LiabilitySummary zero() {
        return new LiabilitySummary(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO
        );
    }
}
