package com.learning.backendservice.engine.rules;

import com.learning.backendservice.domain.recon.*;
import com.learning.backendservice.engine.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * GSTR-1 vs GSTR-3B Reconciliation Audit Rule — Section 37 + Section 39, CGST Act 2017.
 *
 * <p>Compares the outward liability declared in GSTR-1 against the tax declared
 * as payable in GSTR-3B Table 6.1 for the same tax period. Mismatches expose the
 * taxpayer to demand notices under Section 73/74.
 *
 * <p><b>Design</b>: Pure adapter. All financial logic lives in
 * {@link Gstr1Vs3bReconciliationService}. This class translates the
 * {@link AuditRule} contract into domain calls and maps results into
 * {@link AuditFinding} instances.
 *
 * <p><b>DB access</b>: None. Tolerance config is pre-resolved by {@code ContextEnricher}
 * and carried in {@code Gstr1Vs3bInput.reconToleranceAmount/Percent}.
 */
@Component
public class Gstr1Vs3bReconciliationRule implements AuditRule<Gstr1Vs3bInput, Gstr1Vs3bResult> {

    private static final Logger log = LoggerFactory.getLogger(Gstr1Vs3bReconciliationRule.class);

    static final String RULE_ID      = "RECON_1_VS_3B";
    static final String DISPLAY_NAME = "GSTR-1 vs GSTR-3B Reconciliation — Section 37 + 39";
    static final String LEGAL_BASIS  = "Section 37 + Section 39, CGST Act 2017";

    private final Gstr1Vs3bReconciliationService reconciler;

    public Gstr1Vs3bReconciliationRule() {
        this(new Gstr1Vs3bReconciliationService());
    }

    public Gstr1Vs3bReconciliationRule(Gstr1Vs3bReconciliationService reconciler) {
        this.reconciler = reconciler;
    }

    @Override public String getRuleId()      { return RULE_ID; }
    @Override public String getName()        { return "GSTR-1 vs GSTR-3B Reconciliation"; }
    @Override public String getDisplayName() { return DISPLAY_NAME; }
    @Override public String getCategory()    { return "RECONCILIATION"; }
    @Override public String getLegalBasis()  { return LEGAL_BASIS; }
    @Override public int    getExecutionOrder() { return 50; }

    @Override
    public Set<AnalysisMode> getApplicableModes() {
        return Set.of(AnalysisMode.GSTR_RULES_ANALYSIS);
    }

    @Override
    public Set<DocumentType> getRequiredDocumentTypes() {
        return Set.of(DocumentType.GSTR_1, DocumentType.GSTR_3B);
    }

    @Override
    public String getDescription() {
        return "Reconciles the outward tax liability declared in GSTR-1 against the tax payable "
             + "declared in GSTR-3B Table 6.1 for the same tax period. Identifies under/over-reporting "
             + "per tax head (IGST, CGST, SGST/UTGST, CESS). Mismatches may expose the taxpayer to "
             + "demand notices under Section 73/74, CGST Act 2017.";
    }

    @Override
    public AuditRuleResult<Gstr1Vs3bResult> execute(Gstr1Vs3bInput input, AuditContext context) {
        log.debug("Gstr1Vs3bReconciliationRule: gstin={}, period={}, g1TotalTax={}, g3bTotalTax={}",
                input.gstin(), input.taxPeriod(),
                input.gstr1Liability().totalTax(),
                input.gstr3bTaxPayable().totalTax());

        Gstr1Vs3bResult result = reconciler.reconcile(input);

        List<AuditFinding> findings = buildFindings(result, input);

        BigDecimal totalImpact = result.deltas().stream()
                .map(d -> d.delta().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Gstr1Vs3bReconciliationRule: gstin={}, period={}, severity={}, totalDelta={}",
                input.gstin(), input.taxPeriod(), result.overallSeverity(), result.totalDelta());

        return new AuditRuleResult<>(findings, result, totalImpact, 1);
    }

    // ── Finding builders ─────────────────────────────────────────────────────

    private List<AuditFinding> buildFindings(Gstr1Vs3bResult result, Gstr1Vs3bInput input) {
        if (result.overallSeverity() == ReconSeverity.MATCH) {
            return List.of(AuditFinding.info(
                    RULE_ID, LEGAL_BASIS,
                    String.format(
                            "GSTR-1 vs GSTR-3B reconciliation for %s: all tax heads match within tolerance. "
                          + "No corrective action required.",
                            input.taxPeriod())
            ));
        }

        List<AuditFinding> findings = new ArrayList<>();
        String compliancePeriod = String.format("FY: %s, Tax Period: %s",
                input.financialYear(), input.taxPeriod());

        for (ReconDelta delta : result.deltas()) {
            if (delta.severity() == ReconSeverity.MATCH) continue;

            String description = String.format(
                    "%s mismatch for %s: GSTR-1 declared ₹%.2f, GSTR-3B declared ₹%.2f, "
                  + "delta = ₹%.2f (%.2f%%). %s",
                    delta.taxHead(), input.taxPeriod(),
                    delta.gstr1Amount(), delta.gstr3bAmount(),
                    delta.delta(), delta.deltaPercent(),
                    delta.delta().compareTo(BigDecimal.ZERO) > 0
                        ? "Under-reported in GSTR-3B — potential demand under Section 73/74."
                        : "Over-reported in GSTR-3B — excess payment; consider DRC-03 adjustment."
            );

            String recommendation = buildRecommendation(delta, input);

            findings.add(new AuditFinding(
                    RULE_ID,
                    delta.severity().toFindingSeverity(),
                    LEGAL_BASIS,
                    compliancePeriod,
                    delta.delta().abs(),
                    description,
                    recommendation,
                    false
            ));
        }

        return findings.isEmpty()
                ? List.of(AuditFinding.info(RULE_ID, LEGAL_BASIS, result.narrative()))
                : findings;
    }

    private String buildRecommendation(ReconDelta delta, Gstr1Vs3bInput input) {
        if (delta.delta().compareTo(BigDecimal.ZERO) > 0) {
            return String.format(
                    "%s under-reporting of ₹%.2f detected. "
                  + "Either file an amendment to GSTR-3B for %s or pay the difference via DRC-03 "
                  + "to avoid demand notice under Section 73/74, CGST Act 2017.",
                    delta.taxHead(), delta.delta().abs(), input.taxPeriod());
        }
        return String.format(
                "%s over-reporting of ₹%.2f detected. "
              + "Verify whether excess GSTR-3B payment can be claimed as refund or adjusted "
              + "in subsequent returns. Review GSTR-1 amendment if the discrepancy is from "
              + "an incorrect invoice declaration.",
                delta.taxHead(), delta.delta().abs());
    }
}
