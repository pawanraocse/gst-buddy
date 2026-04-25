package com.learning.backendservice.domain.recon;

import java.math.BigDecimal;

/**
 * GSTR-3B tax payment summary (Table 6.1).
 *
 * <p>Populated from the {@code table_6_1.tax_payable} section of the GSTR-3B parser output.
 * For RECON_1_VS_3B reconciliation we compare the GSTR-1 declared liability against
 * the gross tax payable declared in GSTR-3B (before ITC deduction).
 *
 * @param igst      IGST declared as tax payable in GSTR-3B Table 6.1 (₹)
 * @param cgst      CGST declared as tax payable in GSTR-3B Table 6.1 (₹)
 * @param sgstUtgst SGST/UTGST declared as tax payable in GSTR-3B Table 6.1 (₹)
 * @param cess      cess declared as tax payable in GSTR-3B Table 6.1 (₹)
 */
public record TaxPaymentSummary(
        BigDecimal igst,
        BigDecimal cgst,
        BigDecimal sgstUtgst,
        BigDecimal cess
) {
    /** Total tax declared payable in GSTR-3B: IGST + CGST + SGST/UTGST + CESS. */
    public BigDecimal totalTax() {
        return igst.add(cgst).add(sgstUtgst).add(cess);
    }

    /** Convenience factory — all-zero (nil GSTR-3B). */
    public static TaxPaymentSummary zero() {
        return new TaxPaymentSummary(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        );
    }
}
