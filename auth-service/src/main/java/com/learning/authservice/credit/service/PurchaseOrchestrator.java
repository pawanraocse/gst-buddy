package com.learning.authservice.credit.service;

import com.learning.authservice.credit.dto.WalletDto;
import com.learning.authservice.credit.entity.ReferenceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates the plan purchase flow:
 * 1. Create Razorpay order
 * 2. Verify payment signature
 * 3. Grant credits (only after successful verification)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseOrchestrator {

    private final PaymentService paymentService;
    private final CreditService creditService;
    private final PlanService planService;

    /**
     * Create a Razorpay order for purchasing a plan.
     */
    public PaymentService.PaymentOrder createOrder(String planName, String userId) {
        return paymentService.createOrder(planName, userId);
    }

    /**
     * Verify the payment and grant credits.
     * Credits are only granted after HMAC signature verification succeeds.
     *
     * @return updated wallet after credit grant
     * @throws IllegalStateException if payment verification fails
     */
    public WalletDto verifyAndGrantCredits(String orderId, String paymentId, String signature,
            String planName, String userId) {
        // 1. Verify payment signature
        var verification = paymentService.verifyPayment(orderId, paymentId, signature);
        if (!verification.verified()) {
            throw new IllegalStateException("Payment verification failed for orderId=" + orderId);
        }

        // 2. Grant credits from plan
        var plan = planService.getActivePlanByName(planName);
        String idempotencyKey = "purchase-" + orderId + "-" + paymentId;

        WalletDto wallet = creditService.grantCredits(
                userId,
                plan.getCredits(),
                ReferenceType.PLAN_PURCHASE,
                orderId,
                idempotencyKey,
                "Plan purchase: " + plan.getDisplayName() + " (" + plan.getCredits() + " credits)");

        log.info("Purchase completed: plan={}, userId={}, orderId={}, creditsGranted={}",
                planName, userId, orderId, plan.getCredits());

        return wallet;
    }
}
