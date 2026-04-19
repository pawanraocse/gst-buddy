package com.learning.backendservice.engine;

import com.learning.backendservice.domain.gstr1.ReliefWindowSnapshot;

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
 */
public record SharedResources(
        Map<String, List<ReliefWindowSnapshot>> reliefWindowsByReturnType,
        Map<String, String> stateCodeLookup
) {
    /** Empty resources — used before enrichment and for backward-compat {@code of()} factory. */
    public static SharedResources empty() {
        return new SharedResources(Map.of(), Map.of());
    }
}
