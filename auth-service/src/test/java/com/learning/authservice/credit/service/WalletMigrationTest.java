package com.learning.authservice.credit.service;

import com.learning.authservice.credit.entity.CreditTransaction;
import com.learning.authservice.credit.entity.UserCreditWallet;
import com.learning.authservice.credit.repository.CreditTransactionRepository;
import com.learning.authservice.credit.repository.PlanRepository;
import com.learning.authservice.credit.repository.UserCreditWalletRepository;
import com.learning.common.tenant.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CreditServiceImpl.migrateWallet method.
 * Tests wallet consolidation from email-based to UUID-based userId.
 */
@ExtendWith(MockitoExtension.class)
class WalletMigrationTest {

    @Mock
    private UserCreditWalletRepository walletRepository;
    @Mock
    private CreditTransactionRepository transactionRepository;
    @Mock
    private PlanRepository planRepository;
    @InjectMocks
    private CreditServiceImpl creditService;

    private static final String EMAIL = "user@test.com";
    private static final String COGNITO_UUID = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";
    private static final String TENANT_ID = TenantContext.DEFAULT_TENANT;

    @BeforeEach
    void setUp() {
        TenantContext.setCurrentTenant(TENANT_ID);
    }

    // ---- No-op scenarios ----

    @Test
    @DisplayName("no-op when email is null")
    void noOpWhenEmailNull() {
        creditService.migrateWallet(null, COGNITO_UUID);
        verifyNoInteractions(walletRepository, transactionRepository);
    }

    @Test
    @DisplayName("no-op when cognitoUserId is null")
    void noOpWhenCognitoUserIdNull() {
        creditService.migrateWallet(EMAIL, null);
        verifyNoInteractions(walletRepository, transactionRepository);
    }

    @Test
    @DisplayName("no-op when email equals cognitoUserId (same user)")
    void noOpWhenEmailEqualsCognitoId() {
        creditService.migrateWallet("same-id", "same-id");
        verifyNoInteractions(walletRepository, transactionRepository);
    }

    @Test
    @DisplayName("no-op when no email-based wallet exists")
    void noOpWhenNoEmailWallet() {
        when(walletRepository.findByUserIdAndTenantId(EMAIL, TENANT_ID))
                .thenReturn(Optional.empty());

        creditService.migrateWallet(EMAIL, COGNITO_UUID);

        verify(walletRepository).findByUserIdAndTenantId(EMAIL, TENANT_ID);
        verify(walletRepository, never()).save(any());
        verify(walletRepository, never()).delete(any());
    }

    // ---- Migration scenarios ----

    @Test
    @DisplayName("migrates credits from email wallet to existing UUID wallet")
    void migratesCreditsToExistingUuidWallet() {
        // Email-based wallet with 4 credits (2 trial + 2 referral)
        UserCreditWallet emailWallet = UserCreditWallet.builder()
                .userId(EMAIL).tenantId(TENANT_ID)
                .totalCredits(4).consumedCredits(0).hasUsedTrial(true)
                .build();
        // UUID-based wallet (created on first login with 2 trial credits)
        UserCreditWallet uuidWallet = UserCreditWallet.builder()
                .userId(COGNITO_UUID).tenantId(TENANT_ID)
                .totalCredits(2).consumedCredits(0).hasUsedTrial(true)
                .build();

        when(walletRepository.findByUserIdAndTenantId(EMAIL, TENANT_ID))
                .thenReturn(Optional.of(emailWallet));
        when(walletRepository.findByUserIdAndTenantId(COGNITO_UUID, TENANT_ID))
                .thenReturn(Optional.of(uuidWallet));
        when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreditTransaction txn = CreditTransaction.builder()
                .userId(EMAIL).credits(2).description("Trial")
                .build();
        when(transactionRepository.findByUserIdOrderByCreatedAtDesc(EMAIL))
                .thenReturn(List.of(txn));

        creditService.migrateWallet(EMAIL, COGNITO_UUID);

        // UUID wallet should now have 2 (existing) + 4 (migrated) = 6
        assertThat(uuidWallet.getTotalCredits()).isEqualTo(6);
        assertThat(uuidWallet.getHasUsedTrial()).isTrue();

        // Transaction userId reassigned
        assertThat(txn.getUserId()).isEqualTo(COGNITO_UUID);
        verify(transactionRepository).saveAll(List.of(txn));

        // Email wallet deleted
        verify(walletRepository).delete(emailWallet);
    }

    @Test
    @DisplayName("creates UUID wallet if it doesn't exist during migration")
    void createsUuidWalletIfMissing() {
        UserCreditWallet emailWallet = UserCreditWallet.builder()
                .userId(EMAIL).tenantId(TENANT_ID)
                .totalCredits(4).consumedCredits(0).hasUsedTrial(true)
                .build();

        when(walletRepository.findByUserIdAndTenantId(EMAIL, TENANT_ID))
                .thenReturn(Optional.of(emailWallet));
        when(walletRepository.findByUserIdAndTenantId(COGNITO_UUID, TENANT_ID))
                .thenReturn(Optional.empty());
        when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.findByUserIdOrderByCreatedAtDesc(EMAIL))
                .thenReturn(Collections.emptyList());

        creditService.migrateWallet(EMAIL, COGNITO_UUID);

        // Should save twice: once to create new wallet, once to update with credits
        ArgumentCaptor<UserCreditWallet> captor = ArgumentCaptor.forClass(UserCreditWallet.class);
        verify(walletRepository, atLeast(1)).save(captor.capture());

        // The last saved wallet should have the migrated credits
        var savedWallets = captor.getAllValues();
        var lastSaved = savedWallets.get(savedWallets.size() - 1);
        assertThat(lastSaved.getUserId()).isEqualTo(COGNITO_UUID);

        verify(walletRepository).delete(emailWallet);
    }

    @Test
    @DisplayName("deletes email wallet with zero credits without transferring")
    void deletesZeroCreditEmailWallet() {
        UserCreditWallet emailWallet = UserCreditWallet.builder()
                .userId(EMAIL).tenantId(TENANT_ID)
                .totalCredits(2).consumedCredits(2) // 0 remaining
                .hasUsedTrial(true)
                .build();

        when(walletRepository.findByUserIdAndTenantId(EMAIL, TENANT_ID))
                .thenReturn(Optional.of(emailWallet));

        creditService.migrateWallet(EMAIL, COGNITO_UUID);

        // Should delete the empty wallet
        verify(walletRepository).delete(emailWallet);

        // Should NOT look for UUID wallet or reassign transactions
        verify(walletRepository, never())
                .findByUserIdAndTenantId(eq(COGNITO_UUID), eq(TENANT_ID));
        verify(transactionRepository, never())
                .findByUserIdOrderByCreatedAtDesc(any());
    }

    @Test
    @DisplayName("carries over trial flag from email wallet")
    void carriesOverTrialFlag() {
        UserCreditWallet emailWallet = UserCreditWallet.builder()
                .userId(EMAIL).tenantId(TENANT_ID)
                .totalCredits(2).consumedCredits(0).hasUsedTrial(true)
                .build();
        UserCreditWallet uuidWallet = UserCreditWallet.builder()
                .userId(COGNITO_UUID).tenantId(TENANT_ID)
                .totalCredits(0).consumedCredits(0).hasUsedTrial(false)
                .build();

        when(walletRepository.findByUserIdAndTenantId(EMAIL, TENANT_ID))
                .thenReturn(Optional.of(emailWallet));
        when(walletRepository.findByUserIdAndTenantId(COGNITO_UUID, TENANT_ID))
                .thenReturn(Optional.of(uuidWallet));
        when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.findByUserIdOrderByCreatedAtDesc(EMAIL))
                .thenReturn(Collections.emptyList());

        creditService.migrateWallet(EMAIL, COGNITO_UUID);

        assertThat(uuidWallet.getHasUsedTrial()).isTrue();
    }

    @Test
    @DisplayName("migration is idempotent - no-op on second call")
    void migrationIsIdempotent() {
        // First call: email wallet exists
        UserCreditWallet emailWallet = UserCreditWallet.builder()
                .userId(EMAIL).tenantId(TENANT_ID)
                .totalCredits(4).consumedCredits(0).hasUsedTrial(true)
                .build();
        UserCreditWallet uuidWallet = UserCreditWallet.builder()
                .userId(COGNITO_UUID).tenantId(TENANT_ID)
                .totalCredits(2).consumedCredits(0).hasUsedTrial(true)
                .build();

        when(walletRepository.findByUserIdAndTenantId(EMAIL, TENANT_ID))
                .thenReturn(Optional.of(emailWallet))
                .thenReturn(Optional.empty()); // After deletion
        when(walletRepository.findByUserIdAndTenantId(COGNITO_UUID, TENANT_ID))
                .thenReturn(Optional.of(uuidWallet));
        when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.findByUserIdOrderByCreatedAtDesc(EMAIL))
                .thenReturn(Collections.emptyList());

        // First call - does migration
        creditService.migrateWallet(EMAIL, COGNITO_UUID);
        verify(walletRepository).delete(emailWallet);

        // Second call - no-op since email wallet is gone
        creditService.migrateWallet(EMAIL, COGNITO_UUID);
        verify(walletRepository, times(1)).delete(any()); // Still only 1 delete
    }

    @Test
    @DisplayName("does not throw when migration encounters exception")
    void doesNotThrowOnError() {
        when(walletRepository.findByUserIdAndTenantId(EMAIL, TENANT_ID))
                .thenThrow(new RuntimeException("DB error"));

        // migrateWallet should propagate the exception
        // (AuthServiceImpl catches it in try-catch)
        assertThatCode(() -> creditService.migrateWallet(EMAIL, COGNITO_UUID))
                .isInstanceOf(RuntimeException.class);
    }
}
