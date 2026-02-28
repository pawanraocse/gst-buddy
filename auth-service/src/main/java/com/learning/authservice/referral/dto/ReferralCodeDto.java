package com.learning.authservice.referral.dto;

/**
 * Referral code response with shareable link.
 *
 * @param referralCode the user's unique referral code
 * @param referralLink full URL for sharing (e.g.,
 *                     https://app.gstbuddy.com/auth/signup?ref=ABC123)
 */
public record ReferralCodeDto(
        String referralCode,
        String referralLink) {
}
