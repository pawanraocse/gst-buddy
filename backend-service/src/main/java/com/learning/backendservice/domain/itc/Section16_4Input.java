package com.learning.backendservice.domain.itc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record Section16_4Input(
        String gstin,
        LocalDate asOnDate,
        LocalDate gstr3bFilingDate,
        LocalDate annualReturnDate,
        List<ItcRow> itcRows
) {
    public record ItcRow(
            String supplierGstin,
            String invoiceNo,
            LocalDate invoiceDate,
            BigDecimal igst,
            BigDecimal cgst,
            BigDecimal sgst,
            BigDecimal cess,
            boolean isDebitNote
    ) {}
}
