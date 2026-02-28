package com.learning.authservice.referral.repository;

import com.learning.authservice.referral.entity.Referral;
import com.learning.authservice.referral.entity.ReferralStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for referral records.
 */
@Repository
public interface ReferralRepository extends JpaRepository<Referral, Long> {

    /**
     * Find the seed referral row for a user (the row where they generated their
     * code).
     * Used to retrieve a user's existing referral code.
     */
    Optional<Referral> findFirstByReferrerUserIdAndRefereeUserIdIsNull(String referrerUserId);

    /**
     * Find a referral by its code for validation during signup.
     * Returns the seed row (referee_user_id is null).
     */
    Optional<Referral> findFirstByReferralCodeAndRefereeUserIdIsNull(String referralCode);

    /**
     * Check if a specific referral code has already been used by a specific
     * referee.
     * Prevents duplicate conversions.
     */
    boolean existsByReferralCodeAndRefereeUserId(String referralCode, String refereeUserId);

    /**
     * Count successful conversions for a referrer.
     */
    long countByReferrerUserIdAndStatus(String referrerUserId, ReferralStatus status);

    /**
     * List all referrals (conversions) for a referrer.
     */
    List<Referral> findByReferrerUserIdAndStatusOrderByConvertedAtDesc(
            String referrerUserId, ReferralStatus status);
}
