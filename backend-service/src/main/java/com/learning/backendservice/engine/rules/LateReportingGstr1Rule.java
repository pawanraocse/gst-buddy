package com.learning.backendservice.engine.rules;

import com.learning.backendservice.domain.gstr1.*;
import com.learning.backendservice.engine.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * GSTR-1 Late Reporting Interest Audit Rule — Section 50(1), CGST Act 2017.
 *
 * <p>Identifies invoices declared in a GSTR-1 that belong to an earlier tax period
 * (invoice date falls in a prior month) and computes Section 50(1) interest at
 * <b>18% p.a.</b> on the net tax liability for the delay period.
 *
 * <p><b>Legal basis</b>:
 * <ul>
 *   <li>Section 37(1) — outward supplies must be declared in the period they were made.</li>
 *   <li>Section 50(1) — interest at 18% p.a. on net tax liability for delay.</li>
 *   <li>Notification 63/2020-CT — interest on net tax liability (post 01-Sep-2020).</li>
 * </ul>
 *
 * <p><b>Design</b>: Pure adapter. All financial logic lives in
 * {@link LateReportingGstr1CalculatorService}.
 */
@Component
public class LateReportingGstr1Rule implements AuditRule<LateReportingGstr1Input, LateReportingGstr1Result> {

    private static final Logger log = LoggerFactory.getLogger(LateReportingGstr1Rule.class);

    static final String RULE_ID      = "LATE_REPORTING_GSTR1";
    static final String DISPLAY_NAME = "Late Reporting of Invoices in GSTR-1 — Section 50(1)";
    static final String LEGAL_BASIS  = "Section 37(1) + Section 50(1), CGST Act 2017; Notification 63/2020-CT";

    /** Max findings listed individually; beyond this, findings are grouped into a summary. */
    private static final int MAX_INDIVIDUAL_FINDINGS = 20;

    private final LateReportingGstr1CalculatorService calculator;

    public LateReportingGstr1Rule(LateReportingGstr1CalculatorService calculator) {
        this.calculator = calculator;
    }

    @Override public String getRuleId()      { return RULE_ID; }
    @Override public String getName()        { return "GSTR-1 Late Reporting Interest"; }
    @Override public String getDisplayName() { return DISPLAY_NAME; }
    @Override public String getCategory()    { return "COMPLIANCE"; }
    @Override public String getLegalBasis()  { return LEGAL_BASIS; }
    @Override public int    getExecutionOrder() { return 20; }

    @Override
    public Set<AnalysisMode> getApplicableModes() {
        return Set.of(AnalysisMode.GSTR_RULES_ANALYSIS);
    }

    @Override
    public Set<DocumentType> getRequiredDocumentTypes() {
        return Set.of(DocumentType.GSTR_1);
    }

    @Override
    public String getDescription() {
        return "Identifies invoices declared in a GSTR-1 that belong to an earlier tax period "
             + "(invoice date month ≠ GSTR-1 filing period) and computes Section 50(1) interest "
             + "at 18% p.a. on the net tax liability for the delay. "
             + "Per Section 37(1), CGST Act 2017 and Notification 63/2020-CT.";
    }

    @Override
    public AuditRuleResult<LateReportingGstr1Result> execute(
            LateReportingGstr1Input input, AuditContext context) {

        log.debug("LateReportingGstr1Rule: gstin={}, period={}, invoiceCount={}",
                input.gstin(), input.gstr1TaxPeriod(), input.invoices().size());

        LateReportingGstr1Result result = calculator.calculate(input);

        List<AuditFinding> findings = buildFindings(result, input);

        log.info("LateReportingGstr1Rule: gstin={}, period={}, belated={}, totalInterest={}",
                input.gstin(), input.gstr1TaxPeriod(),
                result.totalBelated(), result.totalInterest());

        return new AuditRuleResult<>(findings, result, result.totalInterest(), 1);
    }

    // ── Finding builders ─────────────────────────────────────────────────────

    private List<AuditFinding> buildFindings(
            LateReportingGstr1Result result, LateReportingGstr1Input input) {

        if (result.totalBelated() == 0) {
            return List.of(AuditFinding.info(
                    RULE_ID, LEGAL_BASIS,
                    String.format(
                            "All %d invoices in GSTR-1 for %s were declared in their correct tax period. "
                          + "No belated reporting interest applies.",
                            input.invoices().size(), input.gstr1TaxPeriod())
            ));
        }

        // Group into summary finding if too many belated invoices
        if (result.totalBelated() > MAX_INDIVIDUAL_FINDINGS) {
            return List.of(buildSummaryFinding(result, input));
        }

        // Individual findings per belated invoice
        List<AuditFinding> findings = new ArrayList<>();
        String compliancePeriod = String.format("FY: %s, Filed Period: %s",
                input.financialYear(), input.gstr1TaxPeriod());

        for (BelatedInvoice bi : result.belatedInvoices()) {
            String description = String.format(
                    "Invoice %s dated %s (belongs to %s) declared in GSTR-1 for %s — "
                  + "%d day(s) late. Tax: ₹%.2f. Section 50(1) interest: ₹%.2f "
                  + "(%d days × 18%% p.a.). Per Notification 63/2020-CT.",
                    bi.invoice().invoiceNo(),
                    bi.invoice().invoiceDate(),
                    bi.expectedPeriod(),
                    bi.declaredPeriod(),
                    bi.delayDays(),
                    bi.taxAmount(),
                    bi.interestAmount(),
                    bi.delayDays());

            String recommendation = String.format(
                    "Pay interest of ₹%.2f via DRC-03 for invoice %s. "
                  + "Interest computed under Section 50(1), CGST Act 2017 at 18%% p.a. "
                  + "on net tax liability ₹%.2f for %d days of delay.",
                    bi.interestAmount(),
                    bi.invoice().invoiceNo(),
                    bi.taxAmount(),
                    bi.delayDays());

            findings.add(new AuditFinding(
                    RULE_ID,
                    AuditFinding.Severity.HIGH,
                    LEGAL_BASIS,
                    compliancePeriod,
                    bi.interestAmount(),
                    description,
                    recommendation,
                    false
            ));
        }

        return findings;
    }

    private AuditFinding buildSummaryFinding(
            LateReportingGstr1Result result, LateReportingGstr1Input input) {

        String description = String.format(
                "%d invoice(s) in GSTR-1 for %s declared belated. "
              + "Total tax at risk: ₹%.2f. Total Section 50(1) interest: ₹%.2f. "
              + "Per Section 37(1) + Section 50(1), CGST Act 2017 and Notification 63/2020-CT.",
                result.totalBelated(),
                input.gstr1TaxPeriod(),
                result.totalTaxAtRisk(),
                result.totalInterest());

        String recommendation = String.format(
                "Pay aggregate interest of ₹%.2f via DRC-03. "
              + "Review the %d belated invoices in the detailed report and file amendments "
              + "where required. Interest at 18%% p.a. on net tax liability.",
                result.totalInterest(),
                result.totalBelated());

        return new AuditFinding(
                RULE_ID,
                AuditFinding.Severity.HIGH,
                LEGAL_BASIS,
                String.format("FY: %s, Filed Period: %s", input.financialYear(), input.gstr1TaxPeriod()),
                result.totalInterest(),
                description,
                recommendation,
                false
        );
    }
}
