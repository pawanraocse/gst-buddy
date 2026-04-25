package com.learning.backendservice.domain.gstr3b;

import com.learning.backendservice.domain.gstr1.ReliefWindowSnapshot;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Set;

/**
 * Stateless, database-free calculator for GSTR-3B late fees.
 *
 * <p><b>Legal basis:</b> Section 47(2), CGST Act 2017.
 *
 * <p><b>Standard rates:</b>
 * <ul>
 *   <li>Non-Nil: ₹25/day CGST + ₹25/day SGST. Cap: ₹5,000 each head</li>
 *   <li>Nil return: ₹10/day CGST + ₹10/day SGST. Cap: ₹250 each head</li>
 * </ul>
 *
 * <p><b>Due dates (Section 47(2) / Notification 76/2020-CT):</b>
 * <ul>
 *   <li>Monthly filers: 20th of the following month</li>
 *   <li>QRMP — Category A states: 22nd of the month following the quarter</li>
 *   <li>QRMP — Category B states: 24th of the month following the quarter</li>
 * </ul>
 *
 * <p>All arithmetic uses {@code BigDecimal} with {@code HALF_UP} rounding.
 *     <li>Identifies relief windows to adjust due date or apply fee caps</li>
 * </ul>
 */
public class Gstr3bLateFeeCalculatorService {

    // ── Standard rates (Section 47(2)) ───────────────────────────────────────
    private static final BigDecimal NIL_DAILY_CGST    = new BigDecimal("10.00");
    private static final BigDecimal NIL_DAILY_SGST    = new BigDecimal("10.00");
    private static final BigDecimal NIL_CAP_CGST      = new BigDecimal("250.00");
    private static final BigDecimal NIL_CAP_SGST      = new BigDecimal("250.00");

    private static final BigDecimal NORMAL_DAILY_CGST = new BigDecimal("25.00");
    private static final BigDecimal NORMAL_DAILY_SGST = new BigDecimal("25.00");
    private static final BigDecimal NORMAL_CAP_CGST   = new BigDecimal("5000.00");
    private static final BigDecimal NORMAL_CAP_SGST   = new BigDecimal("5000.00");

    /**
     * Category A states — 22nd due date for QRMP filers.
     * All others (Category B) → 24th.
     * Source: Notification No. 76/2020-CT, dated 15-Oct-2020.
     *
     * <p>State code = first two characters of GSTIN.
     */
    public static final Set<String> CATEGORY_A_STATES = Set.of(
            "22", // Chhattisgarh
            "23", // Madhya Pradesh
            "24", // Gujarat
            "27", // Maharashtra
            "29", // Karnataka
            "30", // Goa
            "32", // Kerala
            "33", // Tamil Nadu
            "36", // Telangana
            "37", // Andhra Pradesh
            "25", // Daman & Diu
            "34", // Puducherry
            "26", // Dadra & Nagar Haveli
            "35", // Andaman & Nicobar
            "31"  // Lakshadweep
    );

    /**
     * Compute the GSTR-3B late fee for the given input.
     *
     * @param input pre-built input record
     * @return computed result with CGST/SGST split and delay details
     */
    public Gstr3bLateFeeResult calculate(Gstr3bLateFeeInput input) {
        LocalDate dueDate = resolveDueDate(input.taxPeriod(), input.isQrmp(), input.stateCode());

        long delayDays = Math.max(0L, ChronoUnit.DAYS.between(dueDate, input.filingDate()));

        if (delayDays == 0) {
            return Gstr3bLateFeeResult.onTime(dueDate, input.filingDate());
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

        return new Gstr3bLateFeeResult(
                dueDate, input.filingDate(), (int) delayDays,
                cgstFee, sgstFee, total,
                reliefApplied, appliedRef);
    }

    // ── Due Date Resolution ───────────────────────────────────────────────────

    /**
     * Resolve the statutory GSTR-3B due date for a given tax period.
     *
     * <p>Monthly filers: 20th of the following month.
     * <p>QRMP Category A: 22nd of the month following the quarter.
     * <p>QRMP Category B: 24th of the month following the quarter.
     *
     * <p>Notification 76/2020-CT, dated 15-Oct-2020.
     *
     * @param taxPeriod  the month/quarter this return covers
     * @param isQrmp     true if quarterly QRMP filer
     * @param stateCode  first two chars of GSTIN → determines Category A or B
     * @return statutory due date
     */
    LocalDate resolveDueDate(YearMonth taxPeriod, boolean isQrmp, String stateCode) {
        if (!isQrmp) {
            // Monthly: 20th of the following month
            return taxPeriod.plusMonths(1).atDay(20);
        }

        // QRMP: due date is after quarter end
        YearMonth quarterEnd = resolveQuarterEnd(taxPeriod);
        YearMonth followingMonth = quarterEnd.plusMonths(1);

        boolean isCategoryA = stateCode != null && CATEGORY_A_STATES.contains(stateCode);
        int dueDay = isCategoryA ? 22 : 24;
        return followingMonth.atDay(dueDay);
    }

    /**
     * Returns the YearMonth of the quarter-end containing the given tax period.
     * GST quarters: Apr-Jun, Jul-Sep, Oct-Dec, Jan-Mar.
     */
    private YearMonth resolveQuarterEnd(YearMonth taxPeriod) {
        int month = taxPeriod.getMonthValue();
        int year  = taxPeriod.getYear();
        // Map month → quarter-end month: 1,2,3→3; 4,5,6→6; 7,8,9→9; 10,11,12→12
        int qEndMonth = ((month - 1) / 3 + 1) * 3;
        return YearMonth.of(year, qEndMonth);
    }

    private BigDecimal nvlZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
