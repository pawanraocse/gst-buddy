package com.learning.authservice.credit.service;

import com.learning.authservice.credit.config.RazorpayConfig;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Razorpay implementation of {@link PaymentService}.
 * <p>
 * Handles order creation and HMAC signature verification
 * using the Razorpay Java SDK.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RazorpayPaymentService implements PaymentService {

    private final RazorpayConfig config;
    private final PlanService planService;
    private RazorpayClient razorpayClient;

    @PostConstruct
    public void init() {
        try {
            this.razorpayClient = new RazorpayClient(config.getKeyId(), config.getKeySecret());
            log.info("Razorpay client initialized successfully");
        } catch (RazorpayException e) {
            log.warn("Razorpay client initialization failed: {}. Payment features will be unavailable.",
                    e.getMessage());
        }
    }

    @Override
    public PaymentOrder createOrder(String planName, String userId) {
        if (razorpayClient == null) {
            throw new IllegalStateException("Razorpay client not initialized. Check API key configuration.");
        }

        var plan = planService.getActivePlanByName(planName);

        if (plan.getIsTrial()) {
            throw new IllegalArgumentException("Cannot create payment order for trial plan");
        }

        // Razorpay expects amount in paise (smallest currency unit)
        int amountInPaise = plan.getPriceInr().multiply(BigDecimal.valueOf(100)).intValue();

        try {
            var orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "plan_" + planName + "_" + userId + "_" + System.currentTimeMillis());
            orderRequest.put("notes", new JSONObject()
                    .put("planName", planName)
                    .put("userId", userId)
                    .put("credits", plan.getCredits()));

            Order order = razorpayClient.orders.create(orderRequest);

            log.info("Razorpay order created: orderId={}, plan={}, userId={}", order.get("id"), planName, userId);

            return new PaymentOrder(
                    order.get("id"),
                    "INR",
                    plan.getPriceInr(),
                    order.get("status"));

        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed: plan={}, userId={}, error={}", planName, userId, e.getMessage());
            throw new RuntimeException("Payment order creation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public PaymentVerification verifyPayment(String orderId, String paymentId, String signature) {
        try {
            var attributes = new JSONObject();
            attributes.put("razorpay_order_id", orderId);
            attributes.put("razorpay_payment_id", paymentId);
            attributes.put("razorpay_signature", signature);

            boolean isValid = Utils.verifyPaymentSignature(attributes, config.getKeySecret());

            log.info("Razorpay payment verification: orderId={}, paymentId={}, valid={}",
                    orderId, paymentId, isValid);

            return new PaymentVerification(isValid, paymentId, orderId);

        } catch (RazorpayException e) {
            log.error("Razorpay signature verification failed: orderId={}, error={}", orderId, e.getMessage());
            return new PaymentVerification(false, paymentId, orderId);
        }
    }
}
