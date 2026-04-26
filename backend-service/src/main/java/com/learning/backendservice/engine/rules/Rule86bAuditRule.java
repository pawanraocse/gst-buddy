package com.learning.backendservice.engine.rules;

import com.learning.backendservice.domain.rule86b.Rule86bCalculationService;
import com.learning.backendservice.domain.rule86b.Rule86bInput;
import com.learning.backendservice.domain.rule86b.Rule86bResult;
import com.learning.backendservice.engine.AuditContext;
import com.learning.backendservice.engine.AuditFinding;
import com.learning.backendservice.engine.AuditRule;
import com.learning.backendservice.engine.AuditRuleResult;
import com.learning.backendservice.engine.DocumentType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Flags taxpayers breaching the 1% cash-ledger threshold (Rule 86B).
 */
@Component
public class Rule86bAuditRule implements AuditRule<Rule86bInput, Rule86bResult> {

    public static final String RULE_ID = "RULE_86B_RESTRICTION";

    private final Rule86bCalculationService evaluatorService;

    public Rule86bAuditRule() {
        this(new Rule86bCalculationService());
    }

    public Rule86bAuditRule(Rule86bCalculationService evaluatorService) {
        this.evaluatorService = evaluatorService;
    }

    @Override
    public String getRuleId() {
        return RULE_ID;
    }

    @Override
    public String getName() {
        return "Rule 86B Restriction";
    }

    @Override
    public String getDisplayName() {
        return "1% Cash Ledger Restriction (Rule 86B)";
    }

    @Override
    public String getDescription() {
        return "Checks if 1% of the output tax liability was discharged using the electronic cash ledger for taxpayers with > ₹50 lakh monthly taxable outward supplies.";
    }

    @Override
    public String getCategory() {
        return "COMPLIANCE";
    }

    @Override
    public String getLegalBasis() {
        return "Rule 86B, CGST Rules 2017";
    }

    @Override
    public int getExecutionOrder() {
        return 40;
    }

    @Override
    public Set<DocumentType> getRequiredDocumentTypes() {
        return Set.of(DocumentType.GSTR_3B);
    }

    @Override
    public AuditRuleResult<Rule86bResult> execute(Rule86bInput input, AuditContext context) {
        Rule86bResult result = evaluatorService.evaluate(input);
        List<AuditFinding> findings = new ArrayList<>();

        String compliancePeriod = "FY: " + context.financialYear() + ", As on: " + context.asOnDate();
        BigDecimal totalImpact = BigDecimal.ZERO;

        if (!result.isApplicable()) {
            findings.add(AuditFinding.info(
                    RULE_ID,
                    getLegalBasis(),
                    "Rule 86B is not applicable (taxable outward supplies <= ₹50 lakh, zero liability, or period before 01-Jan-2021 (Notification 94/2020-CT effective date))."
            ));
        } else if (result.hasExemption()) {
            findings.add(AuditFinding.info(
                    RULE_ID,
                    getLegalBasis(),
                    "Rule 86B exemption applied: " + result.exemptionReason()
            ));
        } else if (result.isBreached()) {
            totalImpact = result.cashShortfall();
            findings.add(new AuditFinding(
                    RULE_ID,
                    AuditFinding.Severity.HIGH,
                    getLegalBasis(),
                    compliancePeriod,
                    result.cashShortfall(),
                    String.format("Cash payment (%s%%) is below the required 1%% of output tax payable. Shortfall: ₹%s.",
                            result.actualCashPercent(), result.cashShortfall()),
                    "Deposit the shortfall amount in the cash ledger to comply with Rule 86B.",
                    false
            ));
        } else {
            findings.add(AuditFinding.info(
                    RULE_ID,
                    getLegalBasis(),
                    "Rule 86B compliant. Cash payment meets or exceeds the 1% threshold."
            ));
        }

        if (result.manualAdvisories() != null) {
            for (String advisory : result.manualAdvisories()) {
                findings.add(new AuditFinding(
                        RULE_ID,
                        AuditFinding.Severity.MEDIUM,
                        getLegalBasis(),
                        compliancePeriod,
                        BigDecimal.ZERO,
                        advisory,
                        "Manual verification required.",
                        false
                ));
            }
        }

        return new AuditRuleResult<>(
                findings,
                result,
                totalImpact,
                getCreditsRequired()
        );
    }
}
