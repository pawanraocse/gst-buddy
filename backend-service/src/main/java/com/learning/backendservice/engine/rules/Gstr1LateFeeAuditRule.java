package com.learning.backendservice.engine.rules;

import com.learning.backendservice.domain.gstr1.Gstr1LateFeeCalculatorService;
import com.learning.backendservice.domain.gstr1.Gstr1LateFeeInput;
import com.learning.backendservice.domain.gstr1.Gstr1LateFeeResult;
import com.learning.backendservice.engine.AuditContext;
import com.learning.backendservice.engine.AuditFinding;
import com.learning.backendservice.engine.AuditRule;
import com.learning.backendservice.engine.AuditRuleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * GST GSTR-1 Late Fee Audit Rule — Section 47(1), CGST Act 2017.
 *
 * <p>Computes whether a GSTR-1 was filed after the statutory due date and
 * calculates the resultant late fee liability split as CGST + SGST.
 *
 * <p><b>Design:</b> Pure adapter. All financial logic lives in
 * {@link Gstr1LateFeeCalculatorService}. This class only translates
 * the {@link AuditRule} contract into domain calls and maps the result
 * into {@link AuditFinding} instances.
 *
 * <p><b>DB access:</b> None. The {@code LateFeeReliefWindow} is pre-resolved
 * by the orchestrator and carried inside {@code Gstr1LateFeeInput.reliefWindow()}.
 */
@Component
public class Gstr1LateFeeAuditRule implements AuditRule<Gstr1LateFeeInput, Gstr1LateFeeResult> {

    private static final Logger log = LoggerFactory.getLogger(Gstr1LateFeeAuditRule.class);

    static final String RULE_ID      = "LATE_FEE_GSTR1";
    static final String DISPLAY_NAME = "GSTR-1 Late Fee — Section 47(1)";
    static final String LEGAL_BASIS  = "Section 47(1), CGST Act 2017";

    private final Gstr1LateFeeCalculatorService calculator;

    public Gstr1LateFeeAuditRule() {
        this(new Gstr1LateFeeCalculatorService());
    }

    public Gstr1LateFeeAuditRule(Gstr1LateFeeCalculatorService calculator) {
        this.calculator = calculator;
    }

    @Override public String getRuleId()      { return RULE_ID; }
    @Override public String getName()        { return "GSTR-1 Late Fee"; }
    @Override public String getDisplayName() { return DISPLAY_NAME; }
    @Override public String getCategory()    { return "COMPLIANCE"; }
    @Override public String getLegalBasis()  { return LEGAL_BASIS; }

    @Override
    public java.util.Set<com.learning.backendservice.engine.AnalysisMode> getApplicableModes() {
        return java.util.Set.of(com.learning.backendservice.engine.AnalysisMode.GSTR_RULES_ANALYSIS);
    }

    @Override
    public java.util.Set<com.learning.backendservice.engine.DocumentType> getRequiredDocumentTypes() {
        return java.util.Set.of(com.learning.backendservice.engine.DocumentType.GSTR_1);
    }

    @Override
    public int getExecutionOrder() { return 10; }

    @Override
    public String getDescription() {
        return "Identifies delayed GSTR-1 filings and computes the applicable late fee "
             + "(₹25/day CGST + ₹25/day SGST, max ₹5,000 each; ₹10/day each for Nil returns, max ₹250 each) "
             + "per Section 47(1), CGST Act 2017. Relief windows per CBIC notifications are applied automatically.";
    }

    @Override
    public AuditRuleResult<Gstr1LateFeeResult> execute(Gstr1LateFeeInput input, AuditContext context) {
        log.debug("Gstr1LateFeeAuditRule: gstin={}, period={}, arnDate={}, qrmp={}, nil={}, fy={}",
                input.gstin(), input.taxPeriod(), input.arnDate(),
                input.isQrmp(), input.isNilReturn(), context.financialYear());

        Gstr1LateFeeResult result = calculator.calculate(input);

        AuditFinding finding = result.delayDays() == 0
                ? buildCleanFinding(result, input)
                : buildLateFinding(result, input);

        log.info("Gstr1LateFeeAuditRule: gstin={}, period={}, delay={}d, cgst={}, sgst={}, relief={}",
                input.gstin(), input.taxPeriod(), result.delayDays(),
                result.cgstFee(), result.sgstFee(), result.reliefApplied());

        return new AuditRuleResult<>(List.of(finding), result, result.totalFee(), 1);
    }

    // ── Finding Builders ─────────────────────────────────────────────────────

    private AuditFinding buildCleanFinding(Gstr1LateFeeResult result, Gstr1LateFeeInput input) {
        return AuditFinding.info(
                RULE_ID, LEGAL_BASIS,
                String.format("GSTR-1 for %s filed on time (ARN date: %s, Due: %s). No late fee applicable.",
                        input.taxPeriod(), result.arnDate(), result.dueDate())
        );
    }

    private AuditFinding buildLateFinding(Gstr1LateFeeResult result, Gstr1LateFeeInput input) {
        String compliancePeriod = String.format("FY: %s, Tax Period: %s",
                input.financialYear(), input.taxPeriod());

        String description = buildDescription(result, input);
        String recommendation = buildRecommendation(result, input);

        return new AuditFinding(
                RULE_ID,
                AuditFinding.Severity.HIGH,
                LEGAL_BASIS,
                compliancePeriod,
                result.totalFee(),
                description,
                recommendation,
                false
        );
    }

    private String buildDescription(Gstr1LateFeeResult result, Gstr1LateFeeInput input) {
        String reliefNote = result.reliefApplied()
                ? String.format(" [Relief applied: %s]", result.appliedNotification())
                : "";

        return String.format(
                "GSTR-1 for %s filed %d day(s) late. "
              + "Due: %s | Filed (ARN): %s | "
              + "Late fee: ₹%.2f CGST + ₹%.2f SGST = ₹%.2f total%s. "
              + "Per Section 47(1), CGST Act 2017.",
                input.taxPeriod(),
                result.delayDays(),
                result.dueDate(),
                result.arnDate(),
                result.cgstFee(),
                result.sgstFee(),
                result.totalFee(),
                reliefNote
        );
    }

    private String buildRecommendation(Gstr1LateFeeResult result, Gstr1LateFeeInput input) {
        if (result.reliefApplied()) {
            return String.format(
                    "Late fee of ₹%.2f has been computed under relief %s. "
                  + "Verify the applicable notification and pay accordingly via GST portal under 'Payment of Tax'.",
                    result.totalFee(), result.appliedNotification()
            );
        }
        return String.format(
                "Pay late fee of ₹%.2f (₹%.2f CGST + ₹%.2f SGST) via GST portal under 'Payment of Tax'. "
              + "Late fee is payable for %d day(s) of delay (from %s to %s) at "
              + "₹%s/day CGST + ₹%s/day SGST per Section 47(1), CGST Act 2017.",
                result.totalFee(), result.cgstFee(), result.sgstFee(),
                result.delayDays(), result.dueDate(), result.arnDate(),
                input.isNilReturn() ? "10" : "25",
                input.isNilReturn() ? "10" : "25"
        );
    }
}
