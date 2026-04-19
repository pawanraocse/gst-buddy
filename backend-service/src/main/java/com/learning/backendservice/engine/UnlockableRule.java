package com.learning.backendservice.engine;

import java.util.Set;

/**
 * A rule that would become executable if the user uploaded additional document types.
 *
 * <p>Returned by {@link RuleResolutionEngine#previewUnlockableRules(AuditContext)} and
 * included in the API response so the frontend can show
 * "Upload GSTR-3B to unlock 3 more rules".
 *
 * @param ruleId           the rule identifier
 * @param displayName      human-readable rule name for the UI
 * @param missingDocuments document types the user still needs to upload to unlock this rule
 */
public record UnlockableRule(
        String ruleId,
        String displayName,
        Set<DocumentType> missingDocuments
) {}
