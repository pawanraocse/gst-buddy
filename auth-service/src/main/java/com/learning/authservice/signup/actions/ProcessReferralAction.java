package com.learning.authservice.signup.actions;

import com.learning.authservice.referral.service.ReferralService;
import com.learning.authservice.signup.pipeline.SignupAction;
import com.learning.authservice.signup.pipeline.SignupActionException;
import com.learning.authservice.signup.pipeline.SignupContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Signup pipeline action to process a referral code.
 *
 * <p>
 * Order: 75 (runs after GrantTrialCreditsAction at 70)
 * </p>
 *
 * <p>
 * Validates the referral code and grants reward credits to both
 * the referrer and the referee. Non-blocking: if this fails,
 * signup still succeeds.
 * </p>
 *
 * <p>
 * Supports() returns false when no referral code is present,
 * so this action is automatically skipped for normal signups.
 * </p>
 */
@Component
@Order(75)
@Slf4j
@RequiredArgsConstructor
public class ProcessReferralAction implements SignupAction {

    private final ReferralService referralService;

    @Override
    public String getName() {
        return "ProcessReferral";
    }

    @Override
    public int getOrder() {
        return 75;
    }

    @Override
    public boolean supports(SignupContext ctx) {
        // Only run if a referral code was provided during signup
        return ctx.getReferralCode() != null && !ctx.getReferralCode().isBlank();
    }

    @Override
    public boolean isAlreadyDone(SignupContext ctx) {
        return ctx.isActionCompleted(getName());
    }

    @Override
    public void execute(SignupContext ctx) throws SignupActionException {
        try {
            String userId = ctx.getEmail(); // userId = email in this system
            String referralCode = ctx.getReferralCode();

            log.info("Processing referral code={} for userId={}", referralCode, userId);

            referralService.processReferral(referralCode, userId);

            ctx.setMetadata("referralProcessed", true);
            ctx.setMetadata("referralCode", referralCode);
            log.info("Referral processed successfully: code={}, referee={}", referralCode, userId);

        } catch (Exception e) {
            // Non-blocking: log and continue — signup should never fail because of referral
            log.warn("Failed to process referral code={} for {}: {}. Referral can be retried manually.",
                    ctx.getReferralCode(), ctx.getEmail(), e.getMessage());
            // Don't throw — signup should still succeed
        }
    }

    @Override
    public void rollback(SignupContext ctx) {
        // No-op: referral credits can be manually adjusted via admin if needed
        log.debug("Rollback requested for ProcessReferral — no-op (credits can be adjusted via admin)");
    }
}
