package com.learning.backendservice.engine;

import com.learning.backendservice.domain.gstr1.ReliefWindowSnapshot;
import com.learning.backendservice.domain.gstr2a.GstinStatusSnapshot;
import com.learning.backendservice.domain.rule86b.Rule86bConfigSnapshot;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Pre-loaded shared DB resources for the audit pipeline.
 *
 * <p>Loaded <em>once</em> by {@code ContextEnricher} before rule execution begins.
 * Rules access these via {@link AuditContext#sharedResources()} — they must NOT
 * query the database directly (per the {@link AuditRule} contract).
 *
 * <p>Keys for {@code reliefWindowsByReturnType}:
 * <ul>
 *   <li>{@code "GSTR_1_NIL"} — nil-return GSTR-1 filers</li>
 *   <li>{@code "GSTR_1_NON_NIL"} — taxable GSTR-1 filers</li>
 *   <li>{@code "GSTR_3B_NIL"} / {@code "GSTR_3B_NON_NIL"}</li>
 *   <li>{@code "GSTR_9"} — annual return (NIL/NON_NIL not differentiated)</li>
 * </ul>
 *
 * @param reliefWindowsByReturnType pre-loaded relief windows keyed by return-type + filer-type
 * @param stateCodeLookup           state code → state name lookup (future use)
 * @param reconToleranceAmount      per-tenant reconciliation tolerance in ₹ (default ₹1.00)
 *                                  — a delta within this amount is classified as MATCH
 * @param reconTolerancePercent     per-tenant reconciliation tolerance as a fraction
 *                                  (default 0.0001 = 0.01%) — applied alongside amount tolerance
 * @param rule86bConfig             Rule 86B configuration thresholds and floor
 * @param gstinStatusMap            Supplier GSTIN status map (cache)
 */
public record SharedResources(
        Map<String, List<ReliefWindowSnapshot>> reliefWindowsByReturnType,
        Map<String, String> stateCodeLookup,
        BigDecimal reconToleranceAmount,
        BigDecimal reconTolerancePercent,
        Rule86bConfigSnapshot rule86bConfig,
        Map<String, GstinStatusSnapshot> gstinStatusMap
) {
    /** Default tolerance: ₹1.00 amount, 0.01% percent. */
    private static final BigDecimal DEFAULT_TOLERANCE_AMOUNT  = new BigDecimal("1.00");
    private static final BigDecimal DEFAULT_TOLERANCE_PERCENT = new BigDecimal("0.0001");

    /**
     * Empty resources — used before enrichment and for backward-compat {@code of()} factory.
     * Supplies safe recon-tolerance defaults so callers need no change.
     */
    public static SharedResources empty() {
        return new SharedResources(
                Map.of(), Map.of(),
                DEFAULT_TOLERANCE_AMOUNT, DEFAULT_TOLERANCE_PERCENT,
                Rule86bConfigSnapshot.defaults(),
                Map.of()
        );
    }
}
