package com.learning.backendservice.domain.gstr1;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure calculator for Section 50(1) interest on belated outward supply declarations in GSTR-1.
 *
 * <p><b>Legal basis</b>: Section 37(1), CGST Act 2017 mandates that outward supplies be declared
 * in the return for the period in which they were made. Section 50(1) imposes interest at
 * <b>18% p.a.</b> on the net tax liability for any delay. Notification 63/2020-CT (effective
 * 01-Sep-2020) confirms interest is on the <b>net tax liability</b> (IGST + CGST + SGST + CESS),
 * not on the taxable value.
 *
 * <p><b>Formula</b>:
 * <pre>
 *   interest = taxAmount × (18 / 100) × delayDays / 365
 * </pre>
 * where:
 * <ul>
 *   <li>{@code taxAmount} = invoice.cgst + invoice.sgst + invoice.igst + invoice.cess</li>
 *   <li>{@code delayDays} = ChronoUnit.DAYS.between(expectedDueDate, declaredDueDate)
 *       — 0 or negative means NOT belated (skipped)</li>
 *   <li>{@code expectedDueDate} = 11th of month following {@code invoiceDate.month}
 *       for monthly filers; 13th of month following quarter-end for QRMP filers</li>
 *   <li>{@code declaredDueDate} = 11th of month following {@code gstr1TaxPeriod}
 *       for monthly filers; 13th of month following quarter-end for QRMP</li>
 * </ul>
 *
 * <p>Design: Pure logic boundary. Extracts reporting delay in days and computes
 * interest liability. No dependency on Spring Context or DB.
 */
public class LateReportingGstr1CalculatorService {

    private static final BigDecimal INTEREST_RATE = new BigDecimal("0.18");
    private static final BigDecimal DAYS_IN_YEAR  = new BigDecimal("365");
    private static final int        SCALE         = 2;
    private static final RoundingMode RM          = RoundingMode.HALF_UP;

    /**
     * Identify belated invoices in a GSTR-1 filing and compute Section 50(1) interest.
     *
     * @param input pre-built input with all invoice rows + GSTR-1 tax period
     * @return result with list of belated invoices and aggregate interest
     */
    public LateReportingGstr1Result calculate(LateReportingGstr1Input input) {
        if (input.invoices() == null || input.invoices().isEmpty()) {
            return LateReportingGstr1Result.clean();
        }

        List<BelatedInvoice> belated = new ArrayList<>();
        BigDecimal totalInterest  = BigDecimal.ZERO;
        BigDecimal totalTaxAtRisk = BigDecimal.ZERO;

        LocalDate declaredDueDate = dueDate(input.gstr1TaxPeriod(), input.isQrmp());

        for (InvoiceRow invoice : input.invoices()) {
            YearMonth expectedPeriod  = invoice.expectedTaxPeriod();

            // Invoice belongs to same or later period — not belated
            if (!expectedPeriod.isBefore(input.gstr1TaxPeriod())) {
                continue;
            }

            LocalDate expectedDueDate = dueDate(expectedPeriod, input.isQrmp());
            long delayDays = ChronoUnit.DAYS.between(expectedDueDate, declaredDueDate);

            // Negative or zero delayDays — filing was timely or declaration is ahead
            if (delayDays <= 0) {
                continue;
            }

            BigDecimal taxAmount = invoice.totalTax().setScale(SCALE, RM);
            BigDecimal interest  = computeInterest(taxAmount, delayDays);

            belated.add(new BelatedInvoice(
                    invoice,
                    expectedPeriod,
                    input.gstr1TaxPeriod(),
                    delayDays,
                    taxAmount,
                    interest
            ));

            totalInterest  = totalInterest.add(interest);
            totalTaxAtRisk = totalTaxAtRisk.add(taxAmount);
        }

        return new LateReportingGstr1Result(
                List.copyOf(belated),
                belated.size(),
                totalInterest.setScale(SCALE, RM),
                totalTaxAtRisk.setScale(SCALE, RM)
        );
    }

    // ── Interest formula ─────────────────────────────────────────────────────

    /**
     * Section 50(1), CGST Act 2017 + Notification 63/2020-CT:
     * interest = taxAmount × 18% × delayDays / 365
     */
    private BigDecimal computeInterest(BigDecimal taxAmount, long delayDays) {
        return taxAmount
                .multiply(INTEREST_RATE)
                .multiply(BigDecimal.valueOf(delayDays))
                .divide(DAYS_IN_YEAR, SCALE, RM);
    }

    // ── Due date logic ────────────────────────────────────────────────────────

    /**
     * GSTR-1 due date for a given tax period.
     *
     * <ul>
     *   <li>Monthly filer: 11th of the month following the tax period
     *       (e.g. Apr-2024 → 11-May-2024).</li>
     *   <li>QRMP filer: 13th of the month following the calendar quarter-end
     *       (e.g. Apr-Jun-2024 → 13-Jul-2024, Jul-Sep-2024 → 13-Oct-2024).</li>
     * </ul>
     *
     * <p>Reference: Rule 59(2), CGST Rules 2017 for QRMP; Section 37(1) for monthly.
     */
    private LocalDate dueDate(YearMonth period, boolean isQrmp) {
        if (isQrmp) {
            // QRMP quarterly due date: 13th of month after quarter-end
            YearMonth quarterEnd = quarterEndOf(period);
            return quarterEnd.plusMonths(1).atDay(13);
        }
        // Monthly: 11th of following month
        return period.plusMonths(1).atDay(11);
    }

    /**
     * Returns the last month of the GST quarter containing the given period.
     * GST quarters: Apr–Jun, Jul–Sep, Oct–Dec, Jan–Mar.
     */
    private YearMonth quarterEndOf(YearMonth period) {
        int month = period.getMonthValue();
        // Quarter boundaries (GST FY starts April)
        int quarterEndMonth = switch (month) {
            case 1, 2, 3    -> 3;  // Jan-Mar
            case 4, 5, 6    -> 6;  // Apr-Jun
            case 7, 8, 9    -> 9;  // Jul-Sep
            case 10, 11, 12 -> 12; // Oct-Dec
            default -> throw new IllegalArgumentException("Invalid month: " + month);
        };
        return YearMonth.of(period.getYear(), quarterEndMonth);
    }
}
