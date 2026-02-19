package com.learning.authservice.credit.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for consuming credits during analysis.
 * Called by backend-service after ledger parsing.
 */
public record ConsumeCreditsRequest(
        @NotBlank String userId,
        @Min(1) int credits,
        String referenceId,
        @NotBlank String idempotencyKey) {
}
