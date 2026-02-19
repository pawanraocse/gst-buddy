package com.learning.authservice.credit.controller;

import com.learning.authservice.credit.dto.ConsumeCreditsRequest;
import com.learning.authservice.credit.dto.WalletDto;
import com.learning.authservice.credit.entity.ReferenceType;
import com.learning.authservice.credit.service.CreditService;
import com.learning.common.constants.HeaderNames;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * API for credit wallet operations.
 * <p>
 * - GET /credits: Authenticated user's wallet balance
 * - POST /credits/consume: Internal endpoint for backend-service to deduct
 * credits
 * - POST /credits/grant: Internal endpoint for admin/system to grant credits
 */
@RestController
@RequestMapping("/api/v1/credits")
@RequiredArgsConstructor
@Slf4j
public class CreditController {

    private final CreditService creditService;

    /**
     * Get current user's wallet balance.
     */
    @GetMapping
    public ResponseEntity<WalletDto> getWallet(HttpServletRequest request) {
        String userId = request.getHeader(HeaderNames.USER_ID);
        return ResponseEntity.ok(creditService.getWallet(userId));
    }

    /**
     * Consume credits during analysis (called by backend-service).
     */
    @PostMapping("/consume")
    public ResponseEntity<WalletDto> consumeCredits(@Valid @RequestBody ConsumeCreditsRequest req) {
        WalletDto wallet = creditService.consumeCredits(
                req.userId(), req.credits(), req.referenceId(), req.idempotencyKey());
        return ResponseEntity.ok(wallet);
    }

    /**
     * Grant credits to a user (admin/system operation).
     */
    @PostMapping("/grant")
    public ResponseEntity<WalletDto> grantCredits(
            @RequestParam String userId,
            @RequestParam int credits,
            @RequestParam(defaultValue = "ADMIN_GRANT") ReferenceType referenceType,
            @RequestParam(required = false) String referenceId,
            @RequestParam(required = false) String idempotencyKey,
            @RequestParam(required = false) String description) {
        WalletDto wallet = creditService.grantCredits(
                userId, credits, referenceType, referenceId, idempotencyKey, description);
        return ResponseEntity.ok(wallet);
    }
}
