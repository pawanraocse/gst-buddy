package com.learning.backendservice.engine.rules;

import com.learning.backendservice.domain.gstr9.Gstr9LateFeeCalculatorService;
import com.learning.backendservice.domain.gstr9.Gstr9LateFeeInput;
import com.learning.backendservice.domain.gstr9.Gstr9LateFeeResult;
import com.learning.backendservice.engine.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * AuditRule for calculating GSTR-9 Late fee — Section 47(2), CGST Act 2017.
 * executionOrder = 10
 */
@Component
public class Gstr9LateFeeAuditRule implements AuditRule<Gstr9LateFeeInput, Gstr9LateFeeResult> {

    public static final String RULE_ID = "LATE_FEE_GSTR9";
    public static final String LEGAL_BASIS = "Section 47(2), CGST Act 2017";

    private final Gstr9LateFeeCalculatorService calculator;

    public Gstr9LateFeeAuditRule(Gstr9LateFeeCalculatorService calculator) {
        this.calculator = calculator;
    }

    @Override
    public String getRuleId() {
        return RULE_ID;
    }

    @Override
    public String getName() {
        return "GSTR-9 Late Fee";
    }

    @Override
    public String getDisplayName() {
        return "GSTR-9 Late Fee — Section 47(2)";
    }

    @Override
    public String getDescription() {
        return "Identifies delayed GSTR-9 (Annual Return) filings and calculates applicable late fee "
             + "(₹100/day CGST + ₹100/day SGST) per Section 47(2), CGST Act 2017. "
             + "Cap is 0.50% (0.25% each) of aggregate turnover. Includes amnesty tracking.";
    }

    @Override
    public String getCategory() {
        return "COMPLIANCE";
    }

    @Override
    public String getLegalBasis() {
        return LEGAL_BASIS;
    }

    @Override
    public Set<DocumentType> getRequiredDocumentTypes() {
        return Set.of(DocumentType.GSTR_9);
    }

    @Override
    public int getExecutionOrder() {
        return 10;
    }

    @Override
    public AuditRuleResult<Gstr9LateFeeResult> execute(Gstr9LateFeeInput input, AuditContext context) {
        Gstr9LateFeeResult result = calculator.calculate(input);

        String compliancePeriod = String.format("FY: %s", input.financialYear());

        if (result.isExempt()) {
            AuditFinding finding = AuditFinding.info(
                    RULE_ID, LEGAL_BASIS,
                    String.format("GSTR-9 for FY %s is exempt (turnover <= 2 Cr). Notification 77/2020-CT.",
                            input.financialYear())
            );
            return new AuditRuleResult<>(List.of(finding), result, result.totalFee(), 1);
        }

        if (result.delayDays() == 0) {
            AuditFinding finding = AuditFinding.info(
                    RULE_ID, LEGAL_BASIS,
                    String.format("GSTR-9 for FY %s filed on time. No late fee applicable.", input.financialYear())
            );
            return new AuditRuleResult<>(List.of(finding), result, result.totalFee(), 1);
        }

        String capNote = result.capAssumedFromAggregateTurnover()
                ? " (Cap approx based on aggregate turnover; strict cap uses State turnover)" : "";
        String amnestyNote = result.amnestyApplied() != null
                ? String.format(" [Amnesty applied: %s]", result.amnestyApplied()) : "";

        String description = String.format(
                "GSTR-9 for FY %s filed %d day(s) late. "
              + "Due: %s | Filed: %s | "
              + "Late fee: ₹%.2f CGST + ₹%.2f SGST = ₹%.2f total%s%s. "
              + "Per Section 47(2), CGST Act 2017.",
                input.financialYear(),
                result.delayDays(),
                result.dueDate(),
                result.filingDate(),
                result.cgstFee(),
                result.sgstFee(),
                result.totalFee(),
                amnestyNote,
                capNote);

        String recommendation = String.format(
                "Pay late fee of ₹%.2f via GST portal under 'Payment of Tax'.",
                result.totalFee());

        AuditFinding finding = new AuditFinding(
                RULE_ID,
                AuditFinding.Severity.HIGH,
                LEGAL_BASIS,
                compliancePeriod,
                result.totalFee(),
                description,
                recommendation,
                false
        );

        return new AuditRuleResult<>(List.of(finding), result, result.totalFee(), 1);
    }
}
