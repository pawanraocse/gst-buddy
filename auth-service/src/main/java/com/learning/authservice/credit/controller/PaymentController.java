package com.learning.authservice.credit.controller;

import com.learning.authservice.credit.config.RazorpayConfig;
import com.learning.authservice.credit.dto.WalletDto;
import com.learning.authservice.credit.service.PaymentService;
import com.learning.authservice.credit.service.PurchaseOrchestrator;
import com.learning.common.constants.HeaderNames;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Payment API for plan purchases via Razorpay.
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
     * Returns order details + Razorpay key ID for frontend checkout.
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
     * Called by frontend after successful Razorpay checkout.
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
}
