package com.learning.backendservice.engine;

import java.math.BigDecimal;

/**
 * An individual compliance finding produced by an {@link AuditRule}.
 *
 * <p>Persisted to the {@code audit_findings} table by the orchestrator.
 * Multiple findings can be produced from a single audit run.
 *
 * @param ruleId            copies the rule ID for direct DB filtering without join
 * @param severity          CRITICAL | HIGH | MEDIUM | LOW | INFO
 * @param legalBasis        GST law citation (e.g. "Section 16(2) proviso, Rule 37 CGST Rules")
 * @param compliancePeriod  affected GST period (e.g. "FY: 2024-25, Tax Period: Apr-2024")
 * @param impactAmount      financial impact of this specific finding (₹)
 * @param description       human-readable explanation of the compliance issue
 * @param recommendedAction actionable next step for the taxpayer
 * @param autoFixAvailable  true if the system can generate the corrective filing entry
 */
public record AuditFinding(
        String ruleId,
        Severity severity,
        String legalBasis,
        String compliancePeriod,
        BigDecimal impactAmount,
        String description,
        String recommendedAction,
        boolean autoFixAvailable
) {
    /**
     * Compliance finding severity levels.
     *
     * <ul>
     *   <li>CRITICAL — Demand notice risk; immediate action required (e.g. BREACHED ITC reversal due)
     *   <li>HIGH     — Material exposure; file corrective return ASAP
     *   <li>MEDIUM   — Moderate risk; advisory action recommended
     *   <li>LOW      — Minor discrepancy; track and monitor
     *   <li>INFO     — Informational only (e.g. "all 47 suppliers paid on time")
     * </ul>
     */
    public enum Severity {
        CRITICAL,
        HIGH,
        MEDIUM,
        LOW,
        INFO;

        /** Maps a domain RiskCategory string to a finding Severity. */
        public static Severity fromRiskCategory(String category) {
            return switch (category) {
                case "BREACHED"  -> CRITICAL;
                case "AT_RISK"   -> HIGH;
                case "SAFE"      -> INFO;
                default          -> MEDIUM;
            };
        }
    }

    /**
     * Convenience factory for a clean/no-issue finding.
     * Used when a rule completes successfully with zero impact.
     */
    public static AuditFinding info(String ruleId, String legalBasis, String message) {
        return new AuditFinding(
                ruleId,
                Severity.INFO,
                legalBasis,
                null,
                BigDecimal.ZERO,
                message,
                "No action required.",
                false
        );
    }
}
