package com.learning.backendservice.domain.shared;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Standardized representation of an invoice from the Purchase Register.
 */
public record PurchaseRegisterRow(
        String supplierGstin,
        String invoiceNo,
        LocalDate invoiceDate,
        BigDecimal taxableValue,
        BigDecimal igst,
        BigDecimal cgst,
        BigDecimal sgst,
        BigDecimal cess,
        boolean rcmFlag
) {
    public BigDecimal totalTax() {
        return igst.add(cgst).add(sgst).add(cess);
    }
}
