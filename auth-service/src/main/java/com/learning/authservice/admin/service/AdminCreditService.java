package com.learning.authservice.admin.service;

import com.learning.authservice.admin.dto.AdminDashboardStatsDto;
import com.learning.authservice.admin.dto.AdminTransactionDto;
import com.learning.authservice.admin.dto.AdminUserDetailDto.WalletSummaryDto;
import com.learning.authservice.credit.dto.WalletDto;
import com.learning.authservice.credit.entity.CreditTransaction;
import com.learning.authservice.credit.entity.ReferenceType;
import com.learning.authservice.credit.entity.UserCreditWallet;
import com.learning.authservice.credit.repository.CreditTransactionRepository;
import com.learning.authservice.credit.repository.PlanRepository;
import com.learning.authservice.credit.repository.UserCreditWalletRepository;
import com.learning.authservice.credit.service.CreditService;
import com.learning.authservice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Admin-level credit and dashboard operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminCreditService {

    private final UserCreditWalletRepository walletRepository;
    private final CreditTransactionRepository transactionRepository;
    private final PlanRepository planRepository;
    private final UserRepository userRepository;
    private final CreditService creditService;

    @Transactional(readOnly = true)
    public Page<WalletSummaryDto> listAllWallets(Pageable pageable) {
        return walletRepository.findAll(pageable).map(this::toWalletSummary);
    }

    @Transactional(readOnly = true)
    public List<AdminTransactionDto> getTransactionHistory(String userId) {
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toTransactionDto)
                .toList();
    }

    public WalletDto grantCredits(String userId, int credits, String description, String adminUserId) {
        String idempotencyKey = "admin-grant-" + adminUserId + "-" + UUID.randomUUID();
        String desc = description != null ? description : "Admin grant by " + adminUserId;
        return creditService.grantCredits(
                userId, credits, ReferenceType.ADMIN_GRANT, adminUserId, idempotencyKey, desc);
    }

    @Transactional(readOnly = true)
    public AdminDashboardStatsDto getDashboardStats() {
        var userStats = userRepository.countUsersByStatusGrouped();
        long totalUsers = 0;
        long activeUsers = 0;
        long disabledUsers = 0;
        long invitedUsers = 0;

        for (Object[] row : userStats) {
            String status = (String) row[0];
            long count = (Long) row[1];
            totalUsers += count;
            switch (status) {
                case "ACTIVE" -> activeUsers = count;
                case "DISABLED" -> disabledUsers = count;
                case "INVITED" -> invitedUsers = count;
                default -> { /* ignore */ }
            }
        }

        List<UserCreditWallet> allWallets = walletRepository.findAll();
        long totalGranted = allWallets.stream().mapToLong(UserCreditWallet::getTotalCredits).sum();
        long totalConsumed = allWallets.stream().mapToLong(UserCreditWallet::getConsumedCredits).sum();

        long activePlans = planRepository.findByIsActiveTrueOrderBySortOrderAsc().size();
        long totalTransactions = transactionRepository.count();

        BigDecimal totalRevenue = transactionRepository.findAll().stream()
                .filter(t -> t.getReferenceType() == ReferenceType.PLAN_PURCHASE)
                .map(t -> BigDecimal.valueOf(t.getCredits()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return AdminDashboardStatsDto.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .disabledUsers(disabledUsers)
                .invitedUsers(invitedUsers)
                .totalCreditsGranted(totalGranted)
                .totalCreditsConsumed(totalConsumed)
                .totalRevenueInr(totalRevenue)
                .activePlans(activePlans)
                .totalTransactions(totalTransactions)
                .build();
    }

    private WalletSummaryDto toWalletSummary(UserCreditWallet w) {
        return WalletSummaryDto.builder()
                .total(w.getTotalCredits())
                .used(w.getConsumedCredits())
                .remaining(w.getRemainingCredits())
                .build();
    }

    private AdminTransactionDto toTransactionDto(CreditTransaction t) {
        return AdminTransactionDto.builder()
                .id(t.getId())
                .userId(t.getUserId())
                .type(t.getType().name())
                .credits(t.getCredits())
                .balanceAfter(t.getBalanceAfter())
                .referenceType(t.getReferenceType().name())
                .referenceId(t.getReferenceId())
                .description(t.getDescription())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
