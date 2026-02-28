package com.learning.authservice.referral.service;

import com.learning.authservice.referral.dto.ReferralCodeDto;
import com.learning.authservice.referral.dto.ReferralStatsDto;

/**
 * Service for managing referral codes and processing referral rewards.
 *
 * <p>
 * Key guarantees:
 * </p>
 * <ul>
 * <li>Code generation is idempotent — same code returned on repeat calls</li>
 * <li>Referral processing is idempotent — duplicate conversions are
 * rejected</li>
 * <li>Self-referral is prevented</li>
 * </ul>
 */
public interface ReferralService {

    /**
     * Get or create a referral code for the given user.
     * Idempotent: returns existing code if already generated.
     *
     * @param userId the user requesting their referral code
     * @return referral code and shareable link
     */
    ReferralCodeDto getOrCreateReferralCode(String userId);

    /**
     * Get referral statistics for a user.
     *
     * @param userId the user to get stats for
     * @return total referrals, total credits earned, reward per referral
     */
    ReferralStatsDto getReferralStats(String userId);

    /**
     * Process a referral during signup.
     * Validates the code, prevents self-referral and duplicates,
     * then grants reward credits to both referrer and referee.
     *
     * @param referralCode  the code used during signup
     * @param refereeUserId the new user who signed up with the code
     * @throws IllegalArgumentException if the code is invalid or self-referral
     * @throws IllegalStateException    if the referral was already processed
     */
    void processReferral(String referralCode, String refereeUserId);
}
