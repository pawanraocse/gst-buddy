package com.learning.backendservice.engine;

/**
 * The two high-level analysis modes exposed by the audit pipeline.
 *
 * <p>The user selects a mode before uploading documents. The mode is the
 * primary filter in {@link RuleResolutionEngine} — rules are only considered
 * if their {@code getApplicableModes()} set includes the active mode.
 */
public enum AnalysisMode {

    /**
     * Rule 37 — 180-day ITC reversal analysis.
     * Input: Excel purchase/party ledger (Tally, Busy, etc.).
     * Credits: 1 per run.
     */
    LEDGER_ANALYSIS,

    /**
     * All GST return-based rules — auto-discovered from uploaded document set.
     * Input: GSTR-1 / 3B / 9 / 2A / 2B PDFs or portal JSON exports.
     * Credits: 20 per run (flat rate regardless of number of rules fired).
     */
    GSTR_RULES_ANALYSIS
}
