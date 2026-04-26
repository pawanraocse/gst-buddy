package com.learning.backendservice.domain.gstr1;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * A single invoice row extracted from GSTR-1 Tables 4A (B2B) or 5A (B2CL).
 *
 * <p>Built by {@link com.learning.backendservice.engine.resolvers.LateReportingGstr1InputResolver}
 * from the {@code invoices[]} array in the parser output.
 *
 * @param invoiceNo      supplier invoice number
 * @param invoiceDate    date of the invoice — used to compute the expected tax period
 * @param placeOfSupply  2-digit state code or description (e.g. "29-Karnataka")
 * @param taxableValue   taxable value of the invoice (₹)
 * @param cgst           CGST charged on this invoice (₹)
 * @param sgst           SGST/UTGST charged on this invoice (₹)
 * @param igst           IGST charged on this invoice (₹)
 * @param cess           cess charged on this invoice (₹)
 * @param rate           applicable GST rate (e.g. 18.0 for 18%)
 */
public record InvoiceRow(
        String     invoiceNo,
        LocalDate  invoiceDate,
        String     placeOfSupply,
        BigDecimal taxableValue,
        BigDecimal cgst,
        BigDecimal sgst,
        BigDecimal igst,
        BigDecimal cess,
        BigDecimal rate
) {
    /**
     * The tax period this invoice <em>should have been reported in</em>,
     * derived from the invoice date (month in which the supply occurred).
     *
     * <p>Per Section 37(1), CGST Act 2017: outward supplies must be declared
     * in the return for the period in which they were made (invoice date month).
     */
    public YearMonth expectedTaxPeriod() {
        return YearMonth.from(invoiceDate);
    }

    /**
     * Total tax charged on this invoice: CGST + SGST + IGST + CESS.
     * This is the base for Section 50(1) interest computation per Notification 63/2020-CT.
     */
    public BigDecimal totalTax() {
        return zeroIfNull(cgst)
                .add(zeroIfNull(sgst))
                .add(zeroIfNull(igst))
                .add(zeroIfNull(cess));
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
