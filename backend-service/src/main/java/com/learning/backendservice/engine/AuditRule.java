package com.learning.backendservice.engine;

import java.util.Set;

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
     * Document types required for this rule to be applicable.
     * Used by {@link RuleResolutionEngine} to auto-discover eligible rules per upload.
     *
     * <p>Default: empty set — rule is not filtered by document type (used for mode-based
     * filtering instead, e.g. {@link AnalysisMode#LEDGER_ANALYSIS} rules like Rule 37).
     */
    default Set<DocumentType> getRequiredDocumentTypes() {
        return Set.of();
    }

    /**
     * Analysis modes in which this rule is applicable.
     * Default: {@link AnalysisMode#GSTR_RULES_ANALYSIS}.
     * Rule 37 overrides this to {@link AnalysisMode#LEDGER_ANALYSIS}.
     */
    default Set<AnalysisMode> getApplicableModes() {
        return Set.of(AnalysisMode.GSTR_RULES_ANALYSIS);
    }

    /**
     * Whether this rule can execute given the current context.
     * Default: all required document types are present in the context.
     * Override for rules with extra preconditions (turnover threshold, FY-specific, etc.).
     *
     * @param context the current audit context
     * @return {@code true} if this rule should be included in the pipeline run
     */
    default boolean canExecute(AuditContext context) {
        return context.getAvailableDocumentTypes().containsAll(getRequiredDocumentTypes());
    }

    /**
     * Execution priority within the pipeline. Lower value = runs earlier.
     *
     * <p>Guidelines:
     * <ul>
     *   <li>0–9   = Pre-checks (GSTIN validation, data quality)</li>
     *   <li>10–19 = Late fee rules (Section 47)</li>
     *   <li>20–29 = Interest rules (Section 50)</li>
     *   <li>30–39 = Deadline guards (Section 16(4))</li>
     *   <li>40–49 = Restriction checks (Rule 86B)</li>
     *   <li>50–59 = Reconciliation rules (cross-document)</li>
     *   <li>60–69 = Supplier risk (external GSTIN lookups)</li>
     *   <li>100   = Default — no ordering preference</li>
     * </ul>
     */
    default int getExecutionOrder() {
        return 100;
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
     * @param context immutable execution context (tenantId, userId, asOnDate, FY, documents)
     * @return standardized {@link AuditRuleResult} containing findings and rule-specific output
     */
    AuditRuleResult<O> execute(I input, AuditContext context);
}
