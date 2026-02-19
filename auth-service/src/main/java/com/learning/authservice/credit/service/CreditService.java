package com.learning.authservice.credit.service;

import com.learning.authservice.credit.dto.WalletDto;
import com.learning.authservice.credit.entity.ReferenceType;

/**
 * Core credit management service.
 * All mutation methods are transactional and idempotent.
 */
public interface CreditService {

    /**
     * Get the current wallet balance for a user.
     * Creates a zero-balance wallet if one doesn't exist.
     */
    WalletDto getWallet(String userId);

    /**
     * Grant trial credits (2) to a new user.
     * Idempotent: silently skips if trial was already granted.
     *
     * @return updated wallet
     */
    WalletDto grantTrialCredits(String userId);

    /**
     * Grant credits to a user from any source (plan purchase, admin, promo).
     *
     * @param userId         target user
     * @param credits        number of credits to grant
     * @param referenceType  what triggered this grant
     * @param referenceId    external reference (plan name, order ID, etc.)
     * @param idempotencyKey unique key to prevent duplicates
     * @param description    human-readable reason
     * @return updated wallet
     */
    WalletDto grantCredits(String userId, int credits, ReferenceType referenceType,
            String referenceId, String idempotencyKey, String description);

    /**
     * Consume credits for analysis.
     * Throws
     * {@link com.learning.authservice.credit.exception.InsufficientCreditsException}
     * if the user doesn't have enough credits.
     *
     * @param userId         user performing the analysis
     * @param credits        credits required
     * @param referenceId    analysis run ID
     * @param idempotencyKey unique key to prevent double-deduction
     * @return updated wallet
     */
    WalletDto consumeCredits(String userId, int credits, String referenceId, String idempotencyKey);

    /**
     * Validate that the user has sufficient credits without consuming them.
     * Throws
     * {@link com.learning.authservice.credit.exception.InsufficientCreditsException}
     * if not.
     */
    void validateSufficientCredits(String userId, int requiredCredits);
}
