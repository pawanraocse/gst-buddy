package com.learning.backendservice.engine.rules;

import com.learning.backendservice.domain.pos.PosMismatch;
import com.learning.backendservice.domain.pos.PosValidationInput;
import com.learning.backendservice.domain.pos.PosValidationResult;
import com.learning.backendservice.domain.pos.PosValidationService;
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
 * Validates IGST vs CGST/SGST tax split per invoice against Place of Supply.
 */
@Component
public class PosValidationGstr1AuditRule implements AuditRule<PosValidationInput, PosValidationResult> {

    public static final String RULE_ID = "POS_VALIDATION_GSTR1";
    
    private final PosValidationService validationService;

    public PosValidationGstr1AuditRule() {
        this(new PosValidationService());
    }

    public PosValidationGstr1AuditRule(PosValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    public String getRuleId() {
        return RULE_ID;
    }

    @Override
    public String getName() {
        return "Place of Supply Validation";
    }

    @Override
    public String getDisplayName() {
        return "GSTR-1 Place of Supply vs Tax Split Validation";
    }

    @Override
    public String getDescription() {
        return "Validates that intra-state invoices charge CGST/SGST and inter-state invoices charge IGST based on the Place of Supply.";
    }

    @Override
    public String getCategory() {
        return "COMPLIANCE";
    }

    @Override
    public String getLegalBasis() {
        return "Sections 10–13, IGST Act 2017 & Section 7–8, CGST Act 2017";
    }

    @Override
    public int getExecutionOrder() {
        return 50;
    }

    @Override
    public Set<DocumentType> getRequiredDocumentTypes() {
        return Set.of(DocumentType.GSTR_1);
    }

    @Override
    public AuditRuleResult<PosValidationResult> execute(PosValidationInput input, AuditContext context) {
        PosValidationResult result = validationService.validate(input);
        List<AuditFinding> findings = new ArrayList<>();

        String compliancePeriod = "FY: " + context.financialYear() + ", As on: " + context.asOnDate();
        BigDecimal totalImpact = BigDecimal.ZERO;

        if (result.totalMismatches() == 0) {
            findings.add(AuditFinding.info(
                    RULE_ID,
                    getLegalBasis(),
                    String.format("All %d invoices have the correct Place of Supply tax split.", result.totalInvoicesChecked())
            ));
        } else {
            for (PosMismatch mismatch : result.mismatches()) {
                String desc;
                BigDecimal impactAmount;
                
                if (mismatch.mismatchType() == PosMismatch.MismatchType.INTRASTATE_SUPPLY_WITH_IGST) {
                    desc = String.format("Invoice %s has intra-state POS (%s) but charged IGST. Should charge CGST+SGST.", 
                            mismatch.invoiceNo(), mismatch.placeOfSupply());
                    impactAmount = mismatch.igst() != null ? mismatch.igst() : BigDecimal.ZERO;
                } else {
                    desc = String.format("Invoice %s has inter-state POS (%s) but charged CGST/SGST. Should charge IGST.", 
                            mismatch.invoiceNo(), mismatch.placeOfSupply());
                    BigDecimal cgst = mismatch.cgst() != null ? mismatch.cgst() : BigDecimal.ZERO;
                    BigDecimal sgst = mismatch.sgst() != null ? mismatch.sgst() : BigDecimal.ZERO;
                    impactAmount = cgst.add(sgst);
                }

                totalImpact = totalImpact.add(impactAmount);

                findings.add(new AuditFinding(
                        RULE_ID,
                        AuditFinding.Severity.HIGH,
                        getLegalBasis(),
                        compliancePeriod,
                        impactAmount,
                        desc,
                        "Amend invoice in next GSTR-1 to correct the tax split.",
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
