package com.learning.authservice.referral.dto;

/**
 * Referral statistics for a user.
 *
 * @param referralCode       the user's referral code
 * @param totalReferrals     number of successful conversions
 * @param totalCreditsEarned total credits earned from referrals
 * @param rewardPerReferral  credits granted per successful referral
 */
public record ReferralStatsDto(
        String referralCode,
        long totalReferrals,
        long totalCreditsEarned,
        int rewardPerReferral) {
}
