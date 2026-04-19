package com.learning.backendservice.domain.gstr1;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

/**
 * Stateless, database-free calculator for GSTR-1 late fees.
 *
 * <p><b>Legal basis:</b> Section 47(1), CGST Act 2017.
 *
 * <p><b>Standard rates:</b>
 * <ul>
 *   <li>Non-Nil: ₹25/day CGST + ₹25/day SGST. Cap: ₹5,000 CGST + ₹5,000 SGST</li>
 *   <li>Nil return: ₹10/day CGST + ₹10/day SGST. Cap: ₹250 CGST + ₹250 SGST</li>
 * </ul>
 *
 * <p><b>Due dates:</b>
 * <ul>
 *   <li>Monthly filer: 11th of the following month</li>
 *   <li>QRMP filer: 13th of the month succeeding the quarter</li>
 * </ul>
 *
 * <p>All arithmetic uses {@code BigDecimal} with {@code HALF_UP} rounding.
 * Cap is applied per tax head independently — never on the combined total.
 */
@Component
public class Gstr1LateFeeCalculatorService {

    // ── Standard daily rates (Section 47(1), CGST Act 2017) ──────────────────
    private static final BigDecimal NIL_DAILY_CGST     = new BigDecimal("10.00");
    private static final BigDecimal NIL_DAILY_SGST     = new BigDecimal("10.00");
    private static final BigDecimal NIL_CAP_CGST       = new BigDecimal("250.00");
    private static final BigDecimal NIL_CAP_SGST       = new BigDecimal("250.00");

    private static final BigDecimal NORMAL_DAILY_CGST  = new BigDecimal("25.00");
    private static final BigDecimal NORMAL_DAILY_SGST  = new BigDecimal("25.00");
    private static final BigDecimal NORMAL_CAP_CGST    = new BigDecimal("5000.00");
    private static final BigDecimal NORMAL_CAP_SGST    = new BigDecimal("5000.00");

    /**
     * Compute the GSTR-1 late fee for the given input.
     *
     * @param input pre-built input record (gstin, arnDate, taxPeriod, flags, relief)
     * @return computed result with CGST/SGST split and delay details
     */
    public Gstr1LateFeeResult calculate(Gstr1LateFeeInput input) {
        LocalDate dueDate = resolveDueDate(input.taxPeriod(), input.isQrmp());

        // Negative = filed early → clamp to 0 (no negative fee)
        long delayDays = Math.max(0L, ChronoUnit.DAYS.between(dueDate, input.arnDate()));

        if (delayDays == 0) {
            return Gstr1LateFeeResult.onTime(dueDate, input.arnDate());
        }

        // ── Resolve effective rates ───────────────────────────────────────────
        BigDecimal dailyCgst;
        BigDecimal dailySgst;
        BigDecimal capCgst;
        BigDecimal capSgst;
        boolean    reliefApplied = false;
        String     appliedRef    = null;

        ReliefWindowSnapshot relief = input.reliefWindow();
        if (relief != null) {
            // Relief window overrides — null field means waived (treat as ₹0)
            dailyCgst    = nvlZero(relief.feeCgstPerDay());
            dailySgst    = nvlZero(relief.feeSgstPerDay());
            capCgst      = relief.maxCapCgst() != null ? relief.maxCapCgst() : NORMAL_CAP_CGST;
            capSgst      = relief.maxCapSgst() != null ? relief.maxCapSgst() : NORMAL_CAP_SGST;
            reliefApplied = true;
            appliedRef   = relief.notificationNo();
        } else if (input.isNilReturn()) {
            dailyCgst = NIL_DAILY_CGST;
            dailySgst = NIL_DAILY_SGST;
            capCgst   = NIL_CAP_CGST;
            capSgst   = NIL_CAP_SGST;
        } else {
            dailyCgst = NORMAL_DAILY_CGST;
            dailySgst = NORMAL_DAILY_SGST;
            capCgst   = NORMAL_CAP_CGST;
            capSgst   = NORMAL_CAP_SGST;
        }

        // ── Compute fees — cap applied independently per tax head ─────────────
        BigDecimal days    = BigDecimal.valueOf(delayDays);
        BigDecimal cgstFee = dailyCgst.multiply(days).setScale(2, RoundingMode.HALF_UP).min(capCgst);
        BigDecimal sgstFee = dailySgst.multiply(days).setScale(2, RoundingMode.HALF_UP).min(capSgst);
        BigDecimal total   = cgstFee.add(sgstFee);

        return new Gstr1LateFeeResult(
                dueDate,
                input.arnDate(),
                (int) delayDays,
                cgstFee,
                sgstFee,
                total,
                reliefApplied,
                appliedRef
        );
    }

    // ── Due Date Resolution ───────────────────────────────────────────────────

    /**
     * Resolve the statutory GSTR-1 due date for a given tax period.
     *
     * <p>Monthly (turnover > ₹5Cr): 11th of the month following the tax period.
     * QRMP (turnover ≤ ₹5Cr)       : 13th of the month following the quarter.
     *
     * @param taxPeriod the tax month (e.g. Mar-2024)
     * @param isQrmp    true if the taxpayer is a quarterly QRMP filer
     * @return statutory due date per CGST notification
     */
    LocalDate resolveDueDate(YearMonth taxPeriod, boolean isQrmp) {
        if (isQrmp) {
            // Quarter ends in: Mar, Jun, Sep, Dec
            // Due date = 13th of the month following the quarter-end month
            YearMonth quarterEnd = resolveQrmpQuarterEnd(taxPeriod);
            return quarterEnd.plusMonths(1).atDay(13);
        }
        // Monthly: 11th of the following month
        return taxPeriod.plusMonths(1).atDay(11);
    }

    /**
     * Returns the YearMonth of the quarter-end that contains the given tax period.
     * GST quarters: Apr-Jun (Q1), Jul-Sep (Q2), Oct-Dec (Q3), Jan-Mar (Q4).
     */
    private YearMonth resolveQrmpQuarterEnd(YearMonth taxPeriod) {
        int month = taxPeriod.getMonthValue();
        int year  = taxPeriod.getYear();
        // Quarter-end months: 6, 9, 12, 3
        int qEndMonth = ((month - 1) / 3 + 1) * 3;
        int qEndYear  = (qEndMonth == 3 && month > 3) ? year + 1 : year;
        // Handle Apr fiscal year start: month 1,2,3 → quarter ends in March same year
        if (qEndMonth > 12) { qEndMonth -= 12; qEndYear += 1; }
        return YearMonth.of(qEndYear, qEndMonth);
    }

    private BigDecimal nvlZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
