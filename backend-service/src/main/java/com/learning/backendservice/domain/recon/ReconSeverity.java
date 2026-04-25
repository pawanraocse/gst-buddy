package com.learning.backendservice.domain.recon;

/**
 * Severity classification of a per-tax-head reconciliation delta.
 *
 * <p>Thresholds (applied to |deltaPercent| = |delta| / gstr1Amount × 100):
 * <ul>
 *   <li>{@code MATCH}    — delta ≤ tolerance amount AND deltaPercent ≤ tolerance percent</li>
 *   <li>{@code MINOR}    — deltaPercent &lt; 5%  (advisory only)</li>
 *   <li>{@code MATERIAL} — 5% ≤ deltaPercent &lt; 20%  (corrective filing recommended)</li>
 *   <li>{@code CRITICAL} — deltaPercent ≥ 20%  (demand notice risk — immediate action)</li>
 * </ul>
 *
 * <p>A GSTR-1 amount of zero with a non-zero GSTR-3B amount uses GSTR-3B as the denominator.
 * A zero denominator defaults to {@code MATCH} only if both amounts are zero.
 */
public enum ReconSeverity {
    MATCH,
    MINOR,
    MATERIAL,
    CRITICAL;

    /**
     * Maps a {@link ReconSeverity} to the equivalent {@link com.learning.backendservice.engine.AuditFinding.Severity}.
     */
    public com.learning.backendservice.engine.AuditFinding.Severity toFindingSeverity() {
        return switch (this) {
            case CRITICAL -> com.learning.backendservice.engine.AuditFinding.Severity.CRITICAL;
            case MATERIAL -> com.learning.backendservice.engine.AuditFinding.Severity.HIGH;
            case MINOR    -> com.learning.backendservice.engine.AuditFinding.Severity.MEDIUM;
            case MATCH    -> com.learning.backendservice.engine.AuditFinding.Severity.INFO;
        };
    }
}
