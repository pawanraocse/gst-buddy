package com.learning.authservice.credit.controller;

import com.learning.authservice.credit.config.RazorpayConfig;
import com.learning.authservice.credit.dto.WalletDto;
import com.learning.authservice.credit.service.PurchaseOrchestrator;
import com.learning.common.constants.HeaderNames;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Payment API for plan purchases via Razorpay.
 * Supports Standard Checkout (frontend-driven) and Webhook (server-driven) flows.
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PurchaseOrchestrator purchaseOrchestrator;
    private final RazorpayConfig razorpayConfig;

    /**
     * Create a Razorpay order for purchasing a plan.
     * Returns order details + Razorpay key ID for frontend Standard Checkout popup.
     */
    @PostMapping("/create-order")
    public ResponseEntity<Map<String, Object>> createOrder(
            @RequestParam String planName,
            HttpServletRequest request) {
        String userId = request.getHeader(HeaderNames.USER_ID);
        var order = purchaseOrchestrator.createOrder(planName, userId);

        return ResponseEntity.ok(Map.of(
                "orderId", order.orderId(),
                "amount", order.amount(),
                "currency", order.currency(),
                "razorpayKeyId", razorpayConfig.getKeyId()));
    }

    /**
     * Verify Razorpay payment and grant credits.
     * Called by frontend after successful Razorpay Standard Checkout callback.
     */
    @PostMapping("/verify")
    public ResponseEntity<WalletDto> verifyPayment(
            @RequestParam String orderId,
            @RequestParam String paymentId,
            @RequestParam String signature,
            @RequestParam String planName,
            HttpServletRequest request) {
        String userId = request.getHeader(HeaderNames.USER_ID);

        WalletDto wallet = purchaseOrchestrator.verifyAndGrantCredits(
                orderId, paymentId, signature, planName, userId);

        return ResponseEntity.ok(wallet);
    }

    /**
     * Razorpay Webhook endpoint — server-to-server payment confirmation.
     * <p>
     * Safety net: if the user's browser dies before the frontend /verify call,
     * Razorpay will POST here directly. We verify the HMAC signature to reject spoofs.
     * <p>
     * Register in Razorpay Dashboard → Settings → Webhooks:
     *   URL: https://api.gstbuddies.com/auth/api/v1/payments/webhook
     *   Event: payment.captured
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            HttpServletRequest request,
            @RequestHeader(value = "x-razorpay-signature", required = false) String signature,
            @RequestHeader(value = "x-razorpay-event", required = false) String event) {

        if (signature == null || signature.isBlank()) {
            log.warn("Razorpay webhook received without signature — rejected");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        // Read raw body as bytes to preserve byte integrity for HMAC verification
        byte[] bodyBytes;
        try {
            bodyBytes = request.getInputStream().readAllBytes();
        } catch (IOException e) {
            log.error("Failed to read webhook body: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        String rawBody = new String(bodyBytes, StandardCharsets.UTF_8);

        // Cryptographic verification — rejects any spoofed requests
        try {
            boolean valid = Utils.verifyWebhookSignature(rawBody, signature, razorpayConfig.getWebhookSecret());
            if (!valid) {
                log.warn("Razorpay webhook signature FAILED — potential spoof attempt");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        } catch (RazorpayException e) {
            log.error("Razorpay webhook signature error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Process only payment.captured — acknowledge everything else silently
        if ("payment.captured".equals(event)) {
            try {
                JSONObject payload = new JSONObject(rawBody);
                JSONObject paymentEntity = payload
                        .getJSONObject("payload")
                        .getJSONObject("payment")
                        .getJSONObject("entity");

                String paymentId = paymentEntity.getString("id");
                String orderId   = paymentEntity.getString("order_id");

                JSONObject notes = paymentEntity.optJSONObject("notes");
                String planName  = notes != null ? notes.optString("planName", null) : null;
                String userId    = notes != null ? notes.optString("userId", null)    : null;

                if (planName == null || userId == null) {
                    log.error("Webhook payment.captured missing notes — orderId={}", orderId);
                    return ResponseEntity.ok().build(); // Acknowledge to stop Razorpay retries
                }

                purchaseOrchestrator.grantCreditsFromWebhook(orderId, paymentId, planName, userId);
                log.info("Webhook: credits granted — userId={}, plan={}, orderId={}", userId, planName, orderId);

            } catch (Exception e) {
                log.error("Webhook processing error for event={}: {}", event, e.getMessage(), e);
                return ResponseEntity.ok().build(); // Return 200 to stop Razorpay retrying
            }
        } else {
            log.debug("Razorpay webhook event={} acknowledged but not handled", event);
        }

        return ResponseEntity.ok().build();
    }
}
