package com.learning.backendservice.domain.pos;

import java.math.BigDecimal;
import java.util.List;

/**
 * Input for POS validation rule.
 * Contains the supplier's state code and a list of invoices to validate.
 */
public record PosValidationInput(
        String supplierStateCode,
        List<InvoiceData> invoices
) {
    public record InvoiceData(
            String invoiceNo,
            String placeOfSupply,
            BigDecimal igst,
            BigDecimal cgst,
            BigDecimal sgst,
            String tableSection
    ) {}
}
