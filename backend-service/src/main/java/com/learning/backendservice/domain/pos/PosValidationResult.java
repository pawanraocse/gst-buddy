package com.learning.backendservice.domain.pos;

import java.util.List;

/**
 * Result of the POS validation rule.
 */
public record PosValidationResult(
        int totalInvoicesChecked,
        int totalMismatches,
        List<PosMismatch> mismatches
) {}
