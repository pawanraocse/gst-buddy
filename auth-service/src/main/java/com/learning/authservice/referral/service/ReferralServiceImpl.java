package com.learning.authservice.referral.service;

import com.learning.authservice.credit.entity.ReferenceType;
import com.learning.authservice.credit.service.CreditService;
import com.learning.authservice.referral.config.ReferralProperties;
import com.learning.authservice.referral.dto.ReferralCodeDto;
import com.learning.authservice.referral.dto.ReferralStatsDto;
import com.learning.authservice.referral.entity.Referral;
import com.learning.authservice.referral.entity.ReferralStatus;
import com.learning.authservice.referral.repository.ReferralRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Implementation of {@link ReferralService}.
 *
 * <p>
 * Handles referral code generation, validation, and credit rewards.
 * All operations are idempotent and tenant-aware.
 * </p>
 *
 * <p>
 * SOLID Principles:
 * </p>
 * <ul>
 * <li>Single Responsibility: referral logic only; credit operations delegated
 * to CreditService</li>
 * <li>Dependency Inversion: depends on CreditService interface, not
 * implementation</li>
 * <li>Open/Closed: reward amount is externalized via ReferralProperties</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReferralServiceImpl implements ReferralService {

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 8;

    private final ReferralRepository referralRepository;
    private final CreditService creditService;
    private final ReferralProperties referralProperties;

    @Override
    @Transactional
    public ReferralCodeDto getOrCreateReferralCode(String userId) {
        // Idempotent: return existing code if already generated
        var existing = referralRepository.findFirstByReferrerUserIdAndRefereeUserIdIsNull(userId);
        if (existing.isPresent()) {
            String code = existing.get().getReferralCode();
            log.debug("Returning existing referral code for userId={}", userId);
            return toCodeDto(code);
        }

        // Generate a unique code
        String code = generateUniqueCode();

        var referral = Referral.builder()
                .referrerUserId(userId)
                .referralCode(code)
                .status(ReferralStatus.ACTIVE)
                .build();
        referralRepository.save(referral);

        log.info("Generated new referral code for userId={}: {}", userId, code);
        return toCodeDto(code);
    }

    @Override
    @Transactional(readOnly = true)
    public ReferralStatsDto getReferralStats(String userId) {
        // Get or create the code first
        String referralCode = referralRepository
                .findFirstByReferrerUserIdAndRefereeUserIdIsNull(userId)
                .map(Referral::getReferralCode)
                .orElse(null);

        long totalReferrals = referralRepository
                .countByReferrerUserIdAndStatus(userId, ReferralStatus.CONVERTED);

        int rewardPerReferral = referralProperties.getRewardCredits();
        long totalCreditsEarned = totalReferrals * rewardPerReferral;

        return new ReferralStatsDto(
                referralCode,
                totalReferrals,
                totalCreditsEarned,
                rewardPerReferral);
    }

    @Override
    @Transactional
    public void processReferral(String referralCode, String refereeUserId) {
        if (referralCode == null || referralCode.isBlank()) {
            throw new IllegalArgumentException("Referral code must not be blank");
        }

        // Find the seed referral row (the code owner)
        var seedReferral = referralRepository
                .findFirstByReferralCodeAndRefereeUserIdIsNull(referralCode)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid referral code: " + referralCode));

        String referrerUserId = seedReferral.getReferrerUserId();

        // Prevent self-referral
        if (referrerUserId.equals(refereeUserId)) {
            throw new IllegalArgumentException("Cannot use your own referral code");
        }

        // Idempotency: check if this referee already used this code
        if (referralRepository.existsByReferralCodeAndRefereeUserId(referralCode, refereeUserId)) {
            log.info("Referral already processed: code={}, referee={}", referralCode, refereeUserId);
            return;
        }

        // Create the conversion record
        var conversion = Referral.builder()
                .referrerUserId(referrerUserId)
                .refereeUserId(refereeUserId)
                .referralCode(referralCode)
                .status(ReferralStatus.CONVERTED)
                .convertedAt(Instant.now())
                .build();
        referralRepository.save(conversion);

        // Grant credits to both parties
        int rewardCredits = referralProperties.getRewardCredits();
        String referrerIdempotencyKey = "referral-referrer-" + referralCode + "-" + refereeUserId;
        String refereeIdempotencyKey = "referral-referee-" + referralCode + "-" + refereeUserId;

        creditService.grantCredits(
                referrerUserId, rewardCredits, ReferenceType.REFERRAL,
                referralCode, referrerIdempotencyKey,
                "Referral reward: " + rewardCredits + " credits for referring a friend");

        creditService.grantCredits(
                refereeUserId, rewardCredits, ReferenceType.REFERRAL,
                referralCode, refereeIdempotencyKey,
                "Welcome bonus: " + rewardCredits + " credits via referral");

        log.info("Referral processed: code={}, referrer={}, referee={}, credits={}",
                referralCode, referrerUserId, refereeUserId, rewardCredits);
    }

    // ---- Helpers ----

    /**
     * Generate a unique 8-character alphanumeric code.
     * Excludes ambiguous characters (0, O, 1, I) for readability.
     */
    private String generateUniqueCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            ThreadLocalRandom random = ThreadLocalRandom.current();
            for (int i = 0; i < CODE_LENGTH; i++) {
                sb.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
            }
            String code = sb.toString();

            // Ensure uniqueness
            if (referralRepository.findFirstByReferralCodeAndRefereeUserIdIsNull(code).isEmpty()) {
                return code;
            }
            log.debug("Referral code collision on attempt {}, retrying", attempt + 1);
        }
        throw new IllegalStateException("Failed to generate unique referral code after 10 attempts");
    }

    private ReferralCodeDto toCodeDto(String code) {
        String link = referralProperties.getBaseUrl() + "?ref=" + code;
        return new ReferralCodeDto(code, link);
    }
}
