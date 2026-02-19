package com.learning.authservice.signup.actions;

import com.learning.authservice.credit.service.CreditService;
import com.learning.authservice.signup.pipeline.SignupAction;
import com.learning.authservice.signup.pipeline.SignupActionException;
import com.learning.authservice.signup.pipeline.SignupContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Signup pipeline action to grant trial credits.
 *
 * Order: 70 (post-signup setup, after user creation + role assignment)
 *
 * Grants free trial credits from the 'trial' plan.
 * Non-blocking: if this fails, signup still succeeds.
 * Idempotent: uses the trial flag on the wallet.
 */
@Component
@Order(70)
@Slf4j
@RequiredArgsConstructor
public class GrantTrialCreditsAction implements SignupAction {

    private final CreditService creditService;

    @Override
    public String getName() {
        return "GrantTrialCredits";
    }

    @Override
    public int getOrder() {
        return 70;
    }

    @Override
    public boolean supports(SignupContext ctx) {
        // Grant trial credits for all signup types
        return true;
    }

    @Override
    public boolean isAlreadyDone(SignupContext ctx) {
        // CreditService.grantTrialCredits() is itself idempotent,
        // but we can check context metadata for fast skip
        return ctx.isActionCompleted(getName());
    }

    @Override
    public void execute(SignupContext ctx) throws SignupActionException {
        try {
            String userId = ctx.getEmail(); // userId = email in this system
            log.info("Granting trial credits for userId={}", userId);

            var wallet = creditService.grantTrialCredits(userId);

            ctx.setMetadata("trialCredits", wallet.remaining());
            log.info("Trial credits granted: userId={}, remaining={}", userId, wallet.remaining());

        } catch (Exception e) {
            // Non-blocking: log and continue, don't fail signup
            log.warn("Failed to grant trial credits for {}: {}. Credits can be granted later via admin.",
                    ctx.getEmail(), e.getMessage());
            // Don't throw — signup should still succeed
        }
    }

    @Override
    public void rollback(SignupContext ctx) {
        // No-op: credits can be manually revoked via admin if needed
        log.debug("Rollback requested for GrantTrialCredits — no-op (credits can be revoked via admin)");
    }
}
