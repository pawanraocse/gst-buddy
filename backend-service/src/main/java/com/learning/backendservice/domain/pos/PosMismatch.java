package com.learning.backendservice.domain.pos;

import java.math.BigDecimal;

/**
 * Represents a Place of Supply mismatch finding.
 */
public record PosMismatch(
        String invoiceNo,
        String placeOfSupply,
        BigDecimal igst,
        BigDecimal cgst,
        BigDecimal sgst,
        MismatchType mismatchType
) {
    public enum MismatchType {
        INTRASTATE_SUPPLY_WITH_IGST, // Expected CGST/SGST, found IGST
        INTERSTATE_SUPPLY_WITH_CGST_SGST  // Expected IGST, found CGST/SGST
    }
}
