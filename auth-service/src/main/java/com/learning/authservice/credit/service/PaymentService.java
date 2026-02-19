package com.learning.authservice.credit.service;

import java.math.BigDecimal;

/**
 * Abstraction for payment provider operations.
 * Current implementation: Razorpay.
 */
public interface PaymentService {

    /**
     * Represents a created payment order.
     */
    record PaymentOrder(
            String orderId,
            String currency,
            BigDecimal amount,
            String status) {
    }

    /**
     * Represents a verified payment result.
     */
    record PaymentVerification(
            boolean verified,
            String paymentId,
            String orderId) {
    }

    /**
     * Create a payment order for the given plan.
     *
     * @param planName plan to purchase
     * @param userId   user making the purchase
     * @return created order with orderId for frontend checkout
     */
    PaymentOrder createOrder(String planName, String userId);

    /**
     * Verify a completed payment using provider-specific signature verification.
     *
     * @param orderId   the order ID
     * @param paymentId the payment ID from the provider
     * @param signature the payment signature for HMAC verification
     * @return verification result
     */
    PaymentVerification verifyPayment(String orderId, String paymentId, String signature);
}
