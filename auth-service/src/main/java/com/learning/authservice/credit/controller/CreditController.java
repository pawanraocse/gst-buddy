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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * API for credit wallet operations.
 * <p>
 * - GET /credits: Authenticated user's wallet balance (gateway-authenticated)
 * - POST /credits/consume: Internal endpoint for backend-service to deduct credits
 * - POST /credits/grant: Restricted internal endpoint for admin/system to grant credits
 */
@RestController
@RequestMapping("/api/v1/credits")
@RequiredArgsConstructor
@Slf4j
public class CreditController {

    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    private final CreditService creditService;

    @Value("${app.internal.api-key:#{null}}")
    private String internalApiKey;

    @GetMapping
    public ResponseEntity<WalletDto> getWallet(HttpServletRequest request) {
        String userId = requireUserId(request);
        return ResponseEntity.ok(creditService.getWallet(userId));
    }

    @PostMapping("/consume")
    public ResponseEntity<WalletDto> consumeCredits(@Valid @RequestBody ConsumeCreditsRequest req) {
        WalletDto wallet = creditService.consumeCredits(
                req.userId(), req.credits(), req.referenceId(), req.idempotencyKey());
        return ResponseEntity.ok(wallet);
    }

    /**
     * Grant credits to a user. Restricted to internal callers with a valid API key.
     */
    @PostMapping("/grant")
    public ResponseEntity<WalletDto> grantCredits(
            HttpServletRequest request,
            @RequestParam String userId,
            @RequestParam int credits,
            @RequestParam(defaultValue = "ADMIN_GRANT") ReferenceType referenceType,
            @RequestParam(required = false) String referenceId,
            @RequestParam(required = false) String idempotencyKey,
            @RequestParam(required = false) String description) {
        requireInternalApiKey(request);
        WalletDto wallet = creditService.grantCredits(
                userId, credits, referenceType, referenceId, idempotencyKey, description);
        return ResponseEntity.ok(wallet);
    }

    private String requireUserId(HttpServletRequest request) {
        String userId = request.getHeader(HeaderNames.USER_ID);
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing X-User-Id header");
        }
        return userId;
    }

    private void requireInternalApiKey(HttpServletRequest request) {
        if (internalApiKey == null || internalApiKey.isBlank()) {
            log.warn("Internal API key not configured — /credits/grant is disabled");
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Endpoint not available");
        }
        String provided = request.getHeader(INTERNAL_API_KEY_HEADER);
        if (!internalApiKey.equals(provided)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal API key");
        }
    }
}
