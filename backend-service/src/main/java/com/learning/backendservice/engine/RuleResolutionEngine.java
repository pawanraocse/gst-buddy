package com.learning.backendservice.engine;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Auto-discovers which {@link AuditRule} implementations are applicable for a given {@link AuditContext}.
 *
 * <p>Filtering pipeline:
 * <ol>
 *   <li><b>Mode filter</b> — only rules whose {@link AuditRule#getApplicableModes()} include the current mode.
 *   <li><b>canExecute</b> — only rules where {@link AuditRule#canExecute(AuditContext)} is {@code true}
 *       (default: all required document types must be present).
 *   <li><b>Sort</b> — ordered ascending by {@link AuditRule#getExecutionOrder()}.
 * </ol>
 *
 * <p>Also supports a "preview" mode: returns rules that would unlock if the user uploaded
 * additional documents. Used by the frontend to prompt "Upload GSTR-3B to unlock 3 more rules".
 */
@Component
@RequiredArgsConstructor
public class RuleResolutionEngine {

    private final AuditRuleRegistry registry;

    /**
     * Resolve all rules that can execute for the current context, sorted by execution order.
     *
     * @param context current audit context including uploaded document types and analysis mode
     * @return executable rules in execution order (ascending)
     */
    public List<AuditRule<?, ?>> resolveExecutableRules(AuditContext context) {
        return registry.getAllRules().stream()
                .filter(rule -> rule.getApplicableModes().contains(context.analysisMode()))
                .filter(rule -> rule.canExecute(context))
                .sorted(Comparator.comparingInt(AuditRule::getExecutionOrder))
                .toList();
    }

    /**
     * Preview rules that would become executable if the user uploaded additional documents.
     * Returns rules that pass the mode filter but fail {@code canExecute()} due to missing docs.
     *
     * @param context current audit context
     * @return unlockable rules with the specific missing document types
     */
    public List<UnlockableRule> previewUnlockableRules(AuditContext context) {
        Set<DocumentType> available = context.getAvailableDocumentTypes();
        return registry.getAllRules().stream()
                .filter(rule -> rule.getApplicableModes().contains(context.analysisMode()))
                .filter(rule -> !rule.canExecute(context))
                .map(rule -> {
                    Set<DocumentType> missing = new HashSet<>(rule.getRequiredDocumentTypes());
                    missing.removeAll(available);
                    return new UnlockableRule(rule.getRuleId(), rule.getDisplayName(), missing);
                })
                .filter(u -> !u.missingDocuments().isEmpty())
                .toList();
    }
}
