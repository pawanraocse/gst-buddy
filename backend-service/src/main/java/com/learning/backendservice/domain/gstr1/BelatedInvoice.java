package com.learning.backendservice.domain.gstr1;

import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * A single invoice that was reported in GSTR-1 later than the period it belongs to.
 *
 * <p>Interest is computed under Section 50(1), CGST Act 2017 and Notification 63/2020-CT
 * on the net tax liability of the invoice at 18% p.a. simple interest for the delay period.
 *
 * @param invoice           the original invoice row
 * @param expectedPeriod    the period this invoice should have been declared in
 * @param declaredPeriod    the period in which the GSTR-1 was actually filed
 * @param delayDays         calendar days between expected due date and declared due date
 *                          (ChronoUnit.DAYS.between(expectedDueDate, declaredDueDate))
 * @param taxAmount         total tax on this invoice — CGST + SGST + IGST + CESS (₹)
 * @param interestAmount    Section 50(1) interest: taxAmount × 18% × delayDays / 365 (₹)
 */
public record BelatedInvoice(
        InvoiceRow invoice,
        YearMonth  expectedPeriod,
        YearMonth  declaredPeriod,
        long       delayDays,
        BigDecimal taxAmount,
        BigDecimal interestAmount
) {}
