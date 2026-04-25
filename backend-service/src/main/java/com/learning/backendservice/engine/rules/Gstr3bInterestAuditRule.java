package com.learning.backendservice.engine.rules;

import com.learning.backendservice.domain.gstr3b.Gstr3bInterestCalculatorService;
import com.learning.backendservice.domain.gstr3b.Gstr3bInterestInput;
import com.learning.backendservice.domain.gstr3b.Gstr3bInterestResult;
import com.learning.backendservice.domain.gstr3b.Gstr3bLateFeeCalculatorService;
import com.learning.backendservice.engine.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * AuditRule for calculating Section 50(1) interest on delayed GSTR-3B filing.
 * Order: 20 (Executed after LATE_FEE_GSTR3B)
 */
@Component
public class Gstr3bInterestAuditRule implements AuditRule<Gstr3bInterestInput, Gstr3bInterestResult> {

    public static final String RULE_ID = "INTEREST_GSTR3B";
    public static final String LEGAL_BASIS = "Section 50(1) of CGST Act, 2017";

    private final Gstr3bInterestCalculatorService calculator;

    public Gstr3bInterestAuditRule() {
        this(new Gstr3bInterestCalculatorService(new Gstr3bLateFeeCalculatorService()));
    }

    public Gstr3bInterestAuditRule(Gstr3bInterestCalculatorService calculator) {
        this.calculator = calculator;
    }

    @Override
    public String getRuleId() {
        return RULE_ID;
    }

    @Override
    public String getName() {
        return "GSTR-3B Interest Sec 50(1)";
    }

    @Override
    public String getDisplayName() {
        return "GSTR-3B Interest — Section 50(1)";
    }

    @Override
    public String getDescription() {
        return "Identifies delayed GSTR-3B filings and evaluates applicable interest "
             + "(@ 18% p.a. on the net cash liability) per Section 50(1), CGST Act 2017. "
             + "Runs parallel to but independent of Section 47 late fee.";
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
        return Set.of(DocumentType.GSTR_3B);
    }

    @Override
    public int getExecutionOrder() {
        return 20; // 20: Interest (after late fee order 10)
    }

    @Override
    public AuditRuleResult<Gstr3bInterestResult> execute(Gstr3bInterestInput input, AuditContext context) {
        Gstr3bInterestResult result = calculator.calculate(input);

        String compliancePeriod = String.format("FY: %s, Tax Period: %s",
                input.financialYear(), input.taxPeriod());

        if (!result.hasDelay()) {
            AuditFinding finding = AuditFinding.info(
                    RULE_ID,
                    LEGAL_BASIS,
                    String.format("GSTR-3B for %s filed on time. No interest applicable. Due: %s, Filed: %s",
                            input.taxPeriod(), result.dueDate(), input.filingDate())
            );
            return new AuditRuleResult<>(List.of(finding), result, result.totalInterest(), 1);
        }

        if (result.requiresManualCashVerification()) {
            AuditFinding finding = new AuditFinding(
                    RULE_ID,
                    AuditFinding.Severity.MEDIUM,
                    LEGAL_BASIS,
                    compliancePeriod,
                    java.math.BigDecimal.ZERO,
                    String.format("GSTR-3B filed late by %d days. Verify net cash liability in Table 6.1 to calculate interest @ 18%% p.a.", result.delayDays()),
                    String.format("Automated calculation skipped as net cash liability is not available in the parsed document. Manually compute interest @ 18%% p.a. for %d days.", result.delayDays()),
                    false
            );
            return new AuditRuleResult<>(List.of(finding), result, result.totalInterest(), 1);
        }

        List<AuditFinding> findings = new ArrayList<>();

        if (result.cgstInterest().compareTo(java.math.BigDecimal.ZERO) > 0) {
            findings.add(new AuditFinding(
                    RULE_ID, AuditFinding.Severity.HIGH, LEGAL_BASIS, compliancePeriod, result.cgstInterest(),
                    String.format("CGST Interest of ₹%.2f applicable for %d days delay.", result.cgstInterest(), result.delayDays()),
                    "Pay applicable CGST interest via DRC-03 or under 'Payment of Tax' in GSTR-3B.",
                    false
            ));
        }

        if (result.sgstInterest().compareTo(java.math.BigDecimal.ZERO) > 0) {
            findings.add(new AuditFinding(
                    RULE_ID, AuditFinding.Severity.HIGH, LEGAL_BASIS, compliancePeriod, result.sgstInterest(),
                    String.format("SGST Interest of ₹%.2f applicable for %d days delay.", result.sgstInterest(), result.delayDays()),
                    "Pay applicable SGST interest via DRC-03 or under 'Payment of Tax' in GSTR-3B.",
                    false
            ));
        }

        if (result.igstInterest().compareTo(java.math.BigDecimal.ZERO) > 0) {
            findings.add(new AuditFinding(
                    RULE_ID, AuditFinding.Severity.HIGH, LEGAL_BASIS, compliancePeriod, result.igstInterest(),
                    String.format("IGST Interest of ₹%.2f applicable for %d days delay.", result.igstInterest(), result.delayDays()),
                    "Pay applicable IGST interest via DRC-03 or under 'Payment of Tax' in GSTR-3B.",
                    false
            ));
        }

        // If delay > 0 but cash paid is exactly zero, we still add an INFO finding to clarify no interest is needed
        if (findings.isEmpty()) {
            findings.add(AuditFinding.info(
                    RULE_ID, LEGAL_BASIS,
                    String.format("GSTR-3B filed late by %d days, but net cash liability is zero. No interest applicable.", result.delayDays())
            ));
        }

        return new AuditRuleResult<>(findings, result, result.totalInterest(), 1);
    }
}
