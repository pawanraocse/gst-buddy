package com.learning.authservice.credit.service;

import com.learning.authservice.credit.dto.WalletDto;
import com.learning.authservice.credit.entity.*;
import com.learning.authservice.credit.exception.InsufficientCreditsException;
import com.learning.authservice.credit.repository.CreditTransactionRepository;
import com.learning.authservice.credit.repository.PlanRepository;
import com.learning.authservice.credit.repository.UserCreditWalletRepository;
import com.learning.common.tenant.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreditServiceImplTest {

        @Mock
        private UserCreditWalletRepository walletRepository;
        @Mock
        private CreditTransactionRepository transactionRepository;
        @Mock
        private PlanRepository planRepository;
        @InjectMocks
        private CreditServiceImpl creditService;

        private static final String USER_ID = "user@test.com";

        @BeforeEach
        void setUp() {
                TenantContext.setCurrentTenant("test-tenant");
        }

        // ---- getWallet ----

        @Nested
        @DisplayName("getWallet")
        class GetWalletTests {

                @Test
                @DisplayName("returns existing wallet balance")
                void returnsExistingWallet() {
                        var wallet = buildWallet(10, 3);
                        when(walletRepository.findByUserIdAndTenantId(USER_ID, "test-tenant"))
                                        .thenReturn(Optional.of(wallet));

                        WalletDto dto = creditService.getWallet(USER_ID);

                        assertThat(dto.total()).isEqualTo(10);
                        assertThat(dto.used()).isEqualTo(3);
                        assertThat(dto.remaining()).isEqualTo(7);
                }

        @Test
        @DisplayName("creates zero-balance wallet if none exists")
        void createsWalletIfMissing() {
            when(walletRepository.findByUserIdAndTenantId(USER_ID, "test-tenant"))
                    .thenReturn(Optional.empty());
            when(walletRepository.save(any(UserCreditWallet.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            WalletDto dto = creditService.getWallet(USER_ID);

            assertThat(dto.total()).isEqualTo(0);
            assertThat(dto.remaining()).isEqualTo(0);
            verify(walletRepository).save(any(UserCreditWallet.class));
        }
        }

        // ---- grantTrialCredits ----

        @Nested
        @DisplayName("grantTrialCredits")
        class GrantTrialCreditsTests {

                @Test
                @DisplayName("grants trial credits on first call")
                void grantsTrialOnFirstCall() {
                        var wallet = buildWallet(0, 0);
                        wallet.setHasUsedTrial(false);

                        var trialPlan = Plan.builder().name("trial").credits(2).build();

                        when(walletRepository.findByUserIdAndTenantId(USER_ID, "test-tenant"))
                                        .thenReturn(Optional.of(wallet));
                        when(planRepository.findByNameAndIsActiveTrue("trial"))
                                        .thenReturn(Optional.of(trialPlan));
                        when(transactionRepository.existsByIdempotencyKey("trial-" + USER_ID))
                                        .thenReturn(false);
                        when(walletRepository.save(any(UserCreditWallet.class)))
                                        .thenAnswer(inv -> inv.getArgument(0));

                        WalletDto dto = creditService.grantTrialCredits(USER_ID);

                        assertThat(dto.total()).isEqualTo(2);
                        assertThat(dto.remaining()).isEqualTo(2);

                        ArgumentCaptor<CreditTransaction> txnCaptor = ArgumentCaptor.forClass(CreditTransaction.class);
                        verify(transactionRepository).save(txnCaptor.capture());
                        assertThat(txnCaptor.getValue().getType()).isEqualTo(TransactionType.GRANT);
                        assertThat(txnCaptor.getValue().getReferenceType()).isEqualTo(ReferenceType.TRIAL);
                        assertThat(txnCaptor.getValue().getCredits()).isEqualTo(2);
                }

                @Test
                @DisplayName("skips if trial already used (idempotent)")
                void skipsIfTrialAlreadyUsed() {
                        var wallet = buildWallet(2, 0);
                        wallet.setHasUsedTrial(true);

                        when(walletRepository.findByUserIdAndTenantId(USER_ID, "test-tenant"))
                                        .thenReturn(Optional.of(wallet));

                        WalletDto dto = creditService.grantTrialCredits(USER_ID);

                        assertThat(dto.total()).isEqualTo(2);
                        verify(transactionRepository, never()).save(any());
                }
        }

        // ---- grantCredits ----

        @Nested
        @DisplayName("grantCredits")
        class GrantCreditsTests {

                @Test
                @DisplayName("grants credits and records transaction")
                void grantsCreditsSuccessfully() {
                        var wallet = buildWallet(2, 1);
                        when(walletRepository.findByUserIdAndTenantId(USER_ID, "test-tenant"))
                                        .thenReturn(Optional.of(wallet));
                        when(transactionRepository.existsByIdempotencyKey("purchase-123"))
                                        .thenReturn(false);
                        when(walletRepository.save(any(UserCreditWallet.class)))
                                        .thenAnswer(inv -> inv.getArgument(0));

                        WalletDto dto = creditService.grantCredits(USER_ID, 5,
                                        ReferenceType.PLAN_PURCHASE, "order-123", "purchase-123", "Pro plan");

                        assertThat(dto.total()).isEqualTo(7); // 2 + 5
                        assertThat(dto.remaining()).isEqualTo(6); // 7 - 1

                        verify(transactionRepository).save(argThat(txn -> txn.getType() == TransactionType.GRANT &&
                                        txn.getCredits() == 5 &&
                                        txn.getIdempotencyKey().equals("purchase-123")));
                }

                @Test
                @DisplayName("rejects zero or negative credits")
                void rejectsInvalidCredits() {
                        assertThatThrownBy(
                                        () -> creditService.grantCredits(USER_ID, 0, ReferenceType.ADMIN_GRANT, null,
                                                        null, null))
                                        .isInstanceOf(IllegalArgumentException.class)
                                        .hasMessageContaining("positive");
                }

        @Test
        @DisplayName("skips duplicate grant (idempotent)")
        void skipsDuplicate() {
            when(transactionRepository.existsByIdempotencyKey("dup-key"))
                    .thenReturn(true);
            var wallet = buildWallet(5, 0);
            when(walletRepository.findByUserIdAndTenantId(USER_ID, "test-tenant"))
                    .thenReturn(Optional.of(wallet));

            WalletDto dto = creditService.grantCredits(USER_ID, 5,
                    ReferenceType.ADMIN_GRANT, null, "dup-key", null);

            assertThat(dto.total()).isEqualTo(5);  // unchanged
            verify(walletRepository, never()).save(any());
        }
        }

        // ---- consumeCredits ----

        @Nested
        @DisplayName("consumeCredits")
        class ConsumeCreditsTests {

                @Test
                @DisplayName("consumes credits when sufficient balance")
                void consumesSuccessfully() {
                        var wallet = buildWallet(5, 1);
                        when(walletRepository.findByUserIdAndTenantId(USER_ID, "test-tenant"))
                                        .thenReturn(Optional.of(wallet));
                        when(transactionRepository.existsByIdempotencyKey("analysis-123"))
                                        .thenReturn(false);
                        when(walletRepository.save(any(UserCreditWallet.class)))
                                        .thenAnswer(inv -> inv.getArgument(0));

                        WalletDto dto = creditService.consumeCredits(USER_ID, 2, "run-42", "analysis-123");

                        assertThat(dto.used()).isEqualTo(3); // 1 + 2
                        assertThat(dto.remaining()).isEqualTo(2); // 5 - 3

                        verify(transactionRepository).save(argThat(txn -> txn.getType() == TransactionType.CONSUME &&
                                        txn.getCredits() == 2 &&
                                        txn.getReferenceType() == ReferenceType.ANALYSIS));
                }

                @Test
                @DisplayName("throws InsufficientCreditsException when balance too low")
                void throwsWhenInsufficient() {
                        var wallet = buildWallet(2, 2); // 0 remaining
                        when(walletRepository.findByUserIdAndTenantId(USER_ID, "test-tenant"))
                                        .thenReturn(Optional.of(wallet));
                        when(transactionRepository.existsByIdempotencyKey("analysis-456"))
                                        .thenReturn(false);

                        assertThatThrownBy(() -> creditService.consumeCredits(USER_ID, 1, "run-99", "analysis-456"))
                                        .isInstanceOf(InsufficientCreditsException.class)
                                        .hasMessageContaining("required=1")
                                        .hasMessageContaining("available=0");
                }

        @Test
        @DisplayName("skips duplicate consumption (idempotent)")
        void skipsDuplicate() {
            when(transactionRepository.existsByIdempotencyKey("dup-consume"))
                    .thenReturn(true);
            var wallet = buildWallet(5, 2);
            when(walletRepository.findByUserIdAndTenantId(USER_ID, "test-tenant"))
                    .thenReturn(Optional.of(wallet));

            WalletDto dto = creditService.consumeCredits(USER_ID, 1, "run-1", "dup-consume");

            assertThat(dto.remaining()).isEqualTo(3); // unchanged
            verify(walletRepository, never()).save(any());
        }
        }

        // ---- validateSufficientCredits ----

        @Nested
        @DisplayName("validateSufficientCredits")
        class ValidateTests {

                @Test
                @DisplayName("passes when credits are sufficient")
                void passesWhenSufficient() {
                        var wallet = buildWallet(5, 2);
                        when(walletRepository.findByUserIdAndTenantId(USER_ID, "test-tenant"))
                                        .thenReturn(Optional.of(wallet));

                        assertThatCode(() -> creditService.validateSufficientCredits(USER_ID, 3))
                                        .doesNotThrowAnyException();
                }

                @Test
                @DisplayName("throws when credits are insufficient")
                void throwsWhenInsufficient() {
                        var wallet = buildWallet(5, 4);
                        when(walletRepository.findByUserIdAndTenantId(USER_ID, "test-tenant"))
                                        .thenReturn(Optional.of(wallet));

                        assertThatThrownBy(() -> creditService.validateSufficientCredits(USER_ID, 2))
                                        .isInstanceOf(InsufficientCreditsException.class);
                }
        }

        // ---- Helpers ----

        private UserCreditWallet buildWallet(int total, int consumed) {
                return UserCreditWallet.builder()
                                .userId(USER_ID)
                                .tenantId("test-tenant")
                                .totalCredits(total)
                                .consumedCredits(consumed)
                                .hasUsedTrial(false)
                                .version(0L)
                                .build();
        }
}
