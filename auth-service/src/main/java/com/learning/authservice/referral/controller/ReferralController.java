package com.learning.authservice.referral.controller;

import com.learning.authservice.referral.dto.ReferralCodeDto;
import com.learning.authservice.referral.dto.ReferralStatsDto;
import com.learning.authservice.referral.service.ReferralService;
import com.learning.common.constants.HeaderNames;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for referral operations.
 *
 * <p>
 * Endpoints require authentication (JWT validated by gateway).
 * User identity is extracted from the {@code X-User-Id} header
 * enriched by the gateway.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/referral")
@RequiredArgsConstructor
@Slf4j
public class ReferralController {

    private final ReferralService referralService;

    /**
     * Get or generate the current user's referral code and shareable link.
     *
     * @param userId user ID from gateway header
     * @return referral code and link
     */
    @GetMapping("/code")
    public ResponseEntity<ReferralCodeDto> getReferralCode(
            @RequestHeader(HeaderNames.USER_ID) String userId) {
        log.debug("Referral code requested by userId={}", userId);
        ReferralCodeDto dto = referralService.getOrCreateReferralCode(userId);
        return ResponseEntity.ok(dto);
    }

    /**
     * Get referral statistics for the current user.
     *
     * @param userId user ID from gateway header
     * @return referral stats (total referrals, credits earned, reward per referral)
     */
    @GetMapping("/stats")
    public ResponseEntity<ReferralStatsDto> getReferralStats(
            @RequestHeader(HeaderNames.USER_ID) String userId) {
        log.debug("Referral stats requested by userId={}", userId);
        ReferralStatsDto stats = referralService.getReferralStats(userId);
        return ResponseEntity.ok(stats);
    }
}
