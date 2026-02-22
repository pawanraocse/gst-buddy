package com.learning.authservice.admin.controller;

import com.learning.authservice.admin.dto.AdminGrantCreditsRequest;
import com.learning.authservice.admin.dto.AdminTransactionDto;
import com.learning.authservice.admin.service.AdminCreditService;
import com.learning.authservice.credit.dto.WalletDto;
import com.learning.authservice.credit.service.CreditService;
import com.learning.common.constants.HeaderNames;
import com.learning.common.infra.security.RequirePermission;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Platform admin endpoints for credit and wallet management.
 */
@RestController
@RequestMapping("/api/v1/admin/credits")
@RequiredArgsConstructor
public class AdminCreditController {

    private final AdminCreditService adminCreditService;
    private final CreditService creditService;

    @GetMapping("/wallets/{userId}")
    @RequirePermission(resource = "credit", action = "read")
    public ResponseEntity<WalletDto> getWallet(@PathVariable String userId) {
        return ResponseEntity.ok(creditService.getWallet(userId));
    }

    @GetMapping("/wallets/{userId}/transactions")
    @RequirePermission(resource = "credit", action = "read")
    public ResponseEntity<List<AdminTransactionDto>> getTransactions(@PathVariable String userId) {
        return ResponseEntity.ok(adminCreditService.getTransactionHistory(userId));
    }

    @PostMapping("/wallets/{userId}/grant")
    @RequirePermission(resource = "credit", action = "manage")
    public ResponseEntity<WalletDto> grantCredits(
            @PathVariable String userId,
            @Valid @RequestBody AdminGrantCreditsRequest request,
            HttpServletRequest httpRequest) {

        String adminId = httpRequest.getHeader(HeaderNames.USER_ID);
        WalletDto wallet = adminCreditService.grantCredits(
                userId, request.credits(), request.description(), adminId);
        return ResponseEntity.ok(wallet);
    }
}
