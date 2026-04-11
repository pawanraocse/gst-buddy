package com.learning.backendservice.engine;

/**
 * Strategy interface for a single GST compliance audit rule.
 *
 * <p>Each GST rule (Rule 37 ITC Reversal, GSTR-1 Late Fee, ITC-2B Matching, etc.)
 * is implemented as a Spring {@code @Component} that implements this interface.
 * The {@link AuditRuleRegistry} auto-discovers all implementations at startup.
 *
 * <p><b>Contract:</b>
 * <ul>
 *   <li>{@link #getRuleId()} must be globally unique across all implementations.
 *   <li>{@link #execute(Object, AuditContext)} must be stateless and thread-safe.
 *   <li>Implementations must NOT access the database or consume credits directly.
 *       These cross-cutting concerns are handled by {@code AuditRunOrchestrator}.
 * </ul>
 *
 * @param <I> Input type (e.g., {@code List<MultipartFile>}, {@code GSTRData})
 * @param <O> Output type (e.g., {@code List<LedgerResult>}, {@code LateFeeResult})
 */
public interface AuditRule<I, O> {

    /**
     * Globally unique rule identifier.
     * Used as the {@code rule_id} column in {@code audit_runs}.
     *
     * <p>Convention: {@code SNAKE_UPPER_CASE}, e.g. {@code RULE_37_ITC_REVERSAL}
     */
    String getRuleId();

    /**
     * Human-readable name (shorter than displayName).
     * Used as a unique identifying name in some UI components.
     */
    String getName();

    /**
     * Human-readable label displayed in the UI rule catalog.
     * Example: {@code "Rule 37 — 180-Day ITC Reversal"}
     */
    String getDisplayName();

    /**
     * Detailed description of what the rule does.
     */
    String getDescription();

    /**
     * Rule category (e.g. COMPLIANCE, RECONCILIATION, ANALYTICS).
     */
    String getCategory();

    /**
     * Legal citation for this rule.
     * Example: {@code "Section 16(2) proviso, Rule 37 CGST Rules, 2017"}
     */
    String getLegalBasis();

    /**
     * Credits consumed per execution.
     * Override to return a different value (e.g., for multi-GSTIN rules).
     * Actual credits are validated and consumed by the orchestrator, not here.
     *
     * @return default 1 credit per run
     */
    default int getCreditsRequired() {
        return 1;
    }

    /**
     * Execute the audit rule.
     *
     * <p><b>Implementations must:</b>
     * <ul>
     *   <li>Be stateless — no instance state should be mutated.
     *   <li>Never call the database or external services directly.
     *   <li>Be OOM-safe — stream or process documents without loading all into memory at once.
     * </ul>
     *
     * @param input   rule-specific input (files, portal data, etc.)
     * @param context immutable execution context (tenantId, userId, asOnDate, FY)
     * @return standardized {@link AuditRuleResult} containing findings and rule-specific output
     */
    AuditRuleResult<O> execute(I input, AuditContext context);
}
