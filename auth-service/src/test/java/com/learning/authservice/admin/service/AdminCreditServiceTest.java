package com.learning.authservice.admin.service;

import com.learning.authservice.admin.dto.AdminDashboardStatsDto;
import com.learning.authservice.admin.dto.AdminTransactionDto;
import com.learning.authservice.credit.dto.WalletDto;
import com.learning.authservice.credit.entity.*;
import com.learning.authservice.credit.repository.CreditTransactionRepository;
import com.learning.authservice.credit.repository.PlanRepository;
import com.learning.authservice.credit.repository.UserCreditWalletRepository;
import com.learning.authservice.credit.service.CreditService;
import com.learning.authservice.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminCreditServiceTest {

    @Mock private UserCreditWalletRepository walletRepository;
    @Mock private CreditTransactionRepository transactionRepository;
    @Mock private PlanRepository planRepository;
    @Mock private UserRepository userRepository;
    @Mock private CreditService creditService;
    @InjectMocks private AdminCreditService adminCreditService;

    private static final String USER_ID = "user-1";
    private static final String ADMIN_ID = "admin-1";

    @Nested
    @DisplayName("getDashboardStats")
    class GetDashboardStatsTests {

        @Test
        @DisplayName("aggregates stats from all repositories")
        void aggregatesStats() {
            when(userRepository.countUsersByStatusGrouped()).thenReturn(List.of(
                    new Object[]{"ACTIVE", 10L},
                    new Object[]{"DISABLED", 3L},
                    new Object[]{"INVITED", 2L}
            ));

            var w1 = UserCreditWallet.builder().totalCredits(50).consumedCredits(20).build();
            var w2 = UserCreditWallet.builder().totalCredits(30).consumedCredits(10).build();
            when(walletRepository.findAll()).thenReturn(List.of(w1, w2));

            when(planRepository.findByIsActiveTrueOrderBySortOrderAsc()).thenReturn(List.of(
                    Plan.builder().name("starter").build(),
                    Plan.builder().name("pro").build()
            ));

            when(transactionRepository.count()).thenReturn(50L);

            var purchaseTx = CreditTransaction.builder()
                    .type(TransactionType.GRANT).referenceType(ReferenceType.PLAN_PURCHASE)
                    .credits(10).build();
            var grantTx = CreditTransaction.builder()
                    .type(TransactionType.GRANT).referenceType(ReferenceType.ADMIN_GRANT)
                    .credits(5).build();
            when(transactionRepository.findAll()).thenReturn(List.of(purchaseTx, grantTx));

            AdminDashboardStatsDto stats = adminCreditService.getDashboardStats();

            assertThat(stats.totalUsers()).isEqualTo(15);
            assertThat(stats.activeUsers()).isEqualTo(10);
            assertThat(stats.disabledUsers()).isEqualTo(3);
            assertThat(stats.invitedUsers()).isEqualTo(2);
            assertThat(stats.totalCreditsGranted()).isEqualTo(80);
            assertThat(stats.totalCreditsConsumed()).isEqualTo(30);
            assertThat(stats.activePlans()).isEqualTo(2);
            assertThat(stats.totalTransactions()).isEqualTo(50);
            assertThat(stats.totalRevenueInr()).isEqualByComparingTo(BigDecimal.TEN);
        }

        @Test
        @DisplayName("handles empty repositories gracefully")
        void handlesEmptyRepos() {
            when(userRepository.countUsersByStatusGrouped()).thenReturn(List.of());
            when(walletRepository.findAll()).thenReturn(List.of());
            when(planRepository.findByIsActiveTrueOrderBySortOrderAsc()).thenReturn(List.of());
            when(transactionRepository.count()).thenReturn(0L);
            when(transactionRepository.findAll()).thenReturn(List.of());

            AdminDashboardStatsDto stats = adminCreditService.getDashboardStats();

            assertThat(stats.totalUsers()).isEqualTo(0);
            assertThat(stats.totalCreditsGranted()).isEqualTo(0);
            assertThat(stats.totalRevenueInr()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("getTransactionHistory")
    class GetTransactionHistoryTests {

        @Test
        @DisplayName("maps transactions to DTOs in descending order")
        void mapsTransactions() {
            var tx = CreditTransaction.builder()
                    .id(1L).userId(USER_ID).type(TransactionType.GRANT)
                    .credits(10).balanceAfter(50)
                    .referenceType(ReferenceType.ADMIN_GRANT).referenceId(ADMIN_ID)
                    .description("bonus").createdAt(Instant.now()).build();

            when(transactionRepository.findByUserIdOrderByCreatedAtDesc(USER_ID))
                    .thenReturn(List.of(tx));

            List<AdminTransactionDto> dtos = adminCreditService.getTransactionHistory(USER_ID);

            assertThat(dtos).hasSize(1);
            assertThat(dtos.get(0).type()).isEqualTo("GRANT");
            assertThat(dtos.get(0).credits()).isEqualTo(10);
            assertThat(dtos.get(0).balanceAfter()).isEqualTo(50);
            assertThat(dtos.get(0).referenceType()).isEqualTo("ADMIN_GRANT");
        }

        @Test
        @DisplayName("returns empty list for user with no transactions")
        void returnsEmptyForNoTransactions() {
            when(transactionRepository.findByUserIdOrderByCreatedAtDesc(USER_ID))
                    .thenReturn(List.of());

            List<AdminTransactionDto> dtos = adminCreditService.getTransactionHistory(USER_ID);

            assertThat(dtos).isEmpty();
        }
    }

    @Nested
    @DisplayName("grantCredits")
    class GrantCreditsTests {

        @Test
        @DisplayName("delegates to CreditService with admin grant reference")
        void delegatesToCreditService() {
            var expectedWallet = new WalletDto(60, 10, 50);
            when(creditService.grantCredits(
                    eq(USER_ID), eq(25), eq(ReferenceType.ADMIN_GRANT),
                    eq(ADMIN_ID), anyString(), eq("Bonus credits"))
            ).thenReturn(expectedWallet);

            WalletDto result = adminCreditService.grantCredits(
                    USER_ID, 25, "Bonus credits", ADMIN_ID);

            assertThat(result.total()).isEqualTo(60);
            assertThat(result.remaining()).isEqualTo(50);
            verify(creditService).grantCredits(
                    eq(USER_ID), eq(25), eq(ReferenceType.ADMIN_GRANT),
                    eq(ADMIN_ID), anyString(), eq("Bonus credits"));
        }

        @Test
        @DisplayName("uses default description when none provided")
        void usesDefaultDescription() {
            when(creditService.grantCredits(
                    eq(USER_ID), eq(10), eq(ReferenceType.ADMIN_GRANT),
                    eq(ADMIN_ID), anyString(), startsWith("Admin grant by"))
            ).thenReturn(new WalletDto(10, 0, 10));

            adminCreditService.grantCredits(USER_ID, 10, null, ADMIN_ID);

            verify(creditService).grantCredits(
                    eq(USER_ID), eq(10), eq(ReferenceType.ADMIN_GRANT),
                    eq(ADMIN_ID), anyString(), eq("Admin grant by " + ADMIN_ID));
        }
    }
}
