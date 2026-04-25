package com.learning.backendservice.engine.rules;

import com.learning.backendservice.domain.gstr3b.Gstr3bLateFeeCalculatorService;
import com.learning.backendservice.domain.gstr3b.Gstr3bLateFeeInput;
import com.learning.backendservice.domain.gstr3b.Gstr3bLateFeeResult;
import com.learning.backendservice.engine.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * GST GSTR-3B Late Fee Audit Rule — Section 47(2), CGST Act 2017.
 *
 * <p>Computes whether a GSTR-3B was filed after the statutory due date and
 * calculates the resultant late fee split as CGST + SGST.
 *
 * <p><b>Due dates (per Notification 76/2020-CT):</b>
 * <ul>
 *   <li>Monthly: 20th of the following month</li>
 *   <li>QRMP Category A states: 22nd of the month following the quarter</li>
 *   <li>QRMP Category B states: 24th of the month following the quarter</li>
 * </ul>
 *
 * <p><b>Rates (Section 47(2)):</b>
 * Non-Nil: ₹25/day CGST + ₹25/day SGST, max ₹5,000 each.
 * Nil: ₹10/day CGST + ₹10/day SGST, max ₹250 each.
 *
 * <p><b>DB access:</b> None. Relief window pre-loaded by {@code ContextEnricher}.
 *
 * <p><b>Note:</b> Section 50 interest runs parallel to this late fee.
 * See {@code Gstr3bInterestAuditRule} — both produce independent findings.
 */
@Component
public class Gstr3bLateFeeAuditRule implements AuditRule<Gstr3bLateFeeInput, Gstr3bLateFeeResult> {

    private static final Logger log = LoggerFactory.getLogger(Gstr3bLateFeeAuditRule.class);

    public static final String RULE_ID      = "LATE_FEE_GSTR3B";
    public static final String DISPLAY_NAME = "GSTR-3B Late Fee — Section 47(2)";
    public static final String LEGAL_BASIS  = "Section 47(2), CGST Act 2017";

    private final Gstr3bLateFeeCalculatorService calculator;

    public Gstr3bLateFeeAuditRule() {
        this(new Gstr3bLateFeeCalculatorService());
    }

    public Gstr3bLateFeeAuditRule(Gstr3bLateFeeCalculatorService calculator) {
        this.calculator = calculator;
    }

    @Override public String getRuleId()      { return RULE_ID; }
    @Override public String getName()        { return "GSTR-3B Late Fee"; }
    @Override public String getDisplayName() { return DISPLAY_NAME; }
    @Override public String getCategory()    { return "COMPLIANCE"; }
    @Override public String getLegalBasis()  { return LEGAL_BASIS; }
    @Override public int getExecutionOrder() { return 10; }

    @Override
    public Set<AnalysisMode> getApplicableModes() {
        return Set.of(AnalysisMode.GSTR_RULES_ANALYSIS);
    }

    @Override
    public Set<DocumentType> getRequiredDocumentTypes() {
        return Set.of(DocumentType.GSTR_3B);
    }

    @Override
    public String getDescription() {
        return "Identifies delayed GSTR-3B filings and computes the applicable late fee "
             + "(₹25/day CGST + ₹25/day SGST, max ₹5,000 each; ₹10/day each for Nil returns, max ₹250 each) "
             + "per Section 47(2), CGST Act 2017. State-aware due dates for QRMP filers "
             + "(22nd for Category A states, 24th for Category B — Notification 76/2020-CT). "
             + "⚠️ Section 50 interest is computed separately by INTEREST_GSTR3B rule.";
    }

    @Override
    public AuditRuleResult<Gstr3bLateFeeResult> execute(Gstr3bLateFeeInput input, AuditContext context) {
        log.debug("Gstr3bLateFeeAuditRule: gstin={}, period={}, filingDate={}, qrmp={}, nil={}, state={}",
                input.gstin(), input.taxPeriod(), input.filingDate(),
                input.isQrmp(), input.isNilReturn(), input.stateCode());

        Gstr3bLateFeeResult result = calculator.calculate(input);

        AuditFinding finding = result.delayDays() == 0
                ? buildCleanFinding(result, input)
                : buildLateFinding(result, input);

        log.info("Gstr3bLateFeeAuditRule: gstin={}, period={}, delay={}d, cgst={}, sgst={}, relief={}",
                input.gstin(), input.taxPeriod(), result.delayDays(),
                result.cgstFee(), result.sgstFee(), result.reliefApplied());

        return new AuditRuleResult<>(List.of(finding), result, result.totalFee(), 1);
    }

    // ── Finding Builders ─────────────────────────────────────────────────────

    private AuditFinding buildCleanFinding(Gstr3bLateFeeResult result, Gstr3bLateFeeInput input) {
        return AuditFinding.info(
                RULE_ID, LEGAL_BASIS,
                String.format("GSTR-3B for %s filed on time (Filing: %s, Due: %s). No late fee applicable.",
                        input.taxPeriod(), result.filingDate(), result.dueDate()));
    }

    private AuditFinding buildLateFinding(Gstr3bLateFeeResult result, Gstr3bLateFeeInput input) {
        String compliancePeriod = String.format("FY: %s, Tax Period: %s",
                input.financialYear(), input.taxPeriod());

        String reliefNote = result.reliefApplied()
                ? String.format(" [Relief: %s]", result.appliedNotification())
                : "";

        String dueDateNote = input.isQrmp()
                ? String.format(" (QRMP %s state — due %s per Notification 76/2020-CT)",
                        Gstr3bLateFeeCalculatorService.CATEGORY_A_STATES.contains(input.stateCode())
                                ? "Category A (22nd)" : "Category B (24th)",
                        result.dueDate())
                : String.format(" (Monthly filer — due 20th, i.e. %s)", result.dueDate());

        String description = String.format(
                "GSTR-3B for %s filed %d day(s) late%s. "
              + "Due: %s | Filed: %s | "
              + "Late fee: ₹%.2f CGST + ₹%.2f SGST = ₹%.2f total%s. "
              + "Per Section 47(2), CGST Act 2017. "
              + "⚠️ Section 50 interest computed separately.",
                input.taxPeriod(),
                result.delayDays(),
                dueDateNote,
                result.dueDate(),
                result.filingDate(),
                result.cgstFee(),
                result.sgstFee(),
                result.totalFee(),
                reliefNote);

        String recommendation = result.reliefApplied()
                ? String.format(
                        "Late fee of ₹%.2f computed under relief %s. "
                      + "Verify applicable notification and pay via GST portal under 'Payment of Tax'.",
                        result.totalFee(), result.appliedNotification())
                : String.format(
                        "Pay late fee of ₹%.2f (₹%.2f CGST + ₹%.2f SGST) via GST portal under 'Payment of Tax'. "
                      + "%d day(s) of delay (from %s to %s) at ₹%s/day CGST + ₹%s/day SGST "
                      + "per Section 47(2), CGST Act 2017.",
                        result.totalFee(), result.cgstFee(), result.sgstFee(),
                        result.delayDays(), result.dueDate(), result.filingDate(),
                        input.isNilReturn() ? "10" : "25",
                        input.isNilReturn() ? "10" : "25");

        return new AuditFinding(
                RULE_ID,
                AuditFinding.Severity.HIGH,
                LEGAL_BASIS,
                compliancePeriod,
                result.totalFee(),
                description,
                recommendation,
                false);
    }
}
