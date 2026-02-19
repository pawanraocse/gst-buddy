package com.learning.authservice.credit.service;

import com.learning.authservice.credit.dto.WalletDto;
import com.learning.authservice.credit.entity.*;
import com.learning.authservice.credit.exception.InsufficientCreditsException;
import com.learning.authservice.credit.repository.CreditTransactionRepository;
import com.learning.authservice.credit.repository.PlanRepository;
import com.learning.authservice.credit.repository.UserCreditWalletRepository;
import com.learning.common.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link CreditService}.
 * <p>
 * Handles credit mutations within database transactions.
 * Uses optimistic locking with retry for concurrent access safety.
 * All credit-granting methods are idempotent via idempotency keys.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CreditServiceImpl implements CreditService {

    private static final int MAX_RETRIES = 3;

    private final UserCreditWalletRepository walletRepository;
    private final CreditTransactionRepository transactionRepository;
    private final PlanRepository planRepository;

    @Override
    @Transactional // Not readOnly — lazily creates wallet on first access
    public WalletDto getWallet(String userId) {
        var wallet = getOrCreateWallet(userId);
        return toDto(wallet);
    }

    @Override
    @Transactional
    public WalletDto grantTrialCredits(String userId) {
        String tenantId = resolveTenantId();
        var wallet = walletRepository.findByUserIdAndTenantId(userId, tenantId)
                .orElseGet(() -> createWallet(userId, tenantId));

        // Idempotent: skip if trial already used
        if (Boolean.TRUE.equals(wallet.getHasUsedTrial())) {
            log.info("Trial credits already granted for userId={}", userId);
            return toDto(wallet);
        }

        // Find trial plan to get credit count
        var trialPlan = planRepository.findByNameAndIsActiveTrue("trial")
                .orElseThrow(() -> new IllegalStateException("Trial plan not found in database"));

        int trialCredits = trialPlan.getCredits();
        String idempotencyKey = "trial-" + userId;

        // Check idempotency
        if (transactionRepository.existsByIdempotencyKey(idempotencyKey)) {
            log.info("Trial credits transaction already exists for userId={}", userId);
            wallet.setHasUsedTrial(true);
            walletRepository.save(wallet);
            return toDto(wallet);
        }

        wallet.addCredits(trialCredits);
        wallet.setHasUsedTrial(true);
        wallet = walletRepository.save(wallet);

        recordTransaction(wallet, TransactionType.GRANT, trialCredits,
                ReferenceType.TRIAL, trialPlan.getName(), idempotencyKey,
                "Trial plan: " + trialCredits + " free credits");

        log.info("Granted {} trial credits to userId={}", trialCredits, userId);
        return toDto(wallet);
    }

    @Override
    @Transactional
    public WalletDto grantCredits(String userId, int credits, ReferenceType referenceType,
            String referenceId, String idempotencyKey, String description) {
        if (credits <= 0) {
            throw new IllegalArgumentException("Credits must be positive, got: " + credits);
        }

        // Idempotency check
        if (idempotencyKey != null && transactionRepository.existsByIdempotencyKey(idempotencyKey)) {
            log.info("Grant already processed: idempotencyKey={}", idempotencyKey);
            return getWallet(userId);
        }

        var wallet = getOrCreateWallet(userId);
        wallet.addCredits(credits);
        wallet = walletRepository.save(wallet);

        recordTransaction(wallet, TransactionType.GRANT, credits,
                referenceType, referenceId, idempotencyKey, description);

        log.info("Granted {} credits to userId={}, refType={}, refId={}",
                credits, userId, referenceType, referenceId);
        return toDto(wallet);
    }

    @Override
    @Transactional
    public WalletDto consumeCredits(String userId, int credits, String referenceId, String idempotencyKey) {
        if (credits <= 0) {
            throw new IllegalArgumentException("Credits must be positive, got: " + credits);
        }

        // Idempotency check
        if (idempotencyKey != null && transactionRepository.existsByIdempotencyKey(idempotencyKey)) {
            log.info("Consumption already processed: idempotencyKey={}", idempotencyKey);
            return getWallet(userId);
        }

        return executeWithOptimisticRetry(() -> {
            var wallet = getOrCreateWallet(userId);

            if (wallet.getRemainingCredits() < credits) {
                throw new InsufficientCreditsException(credits, wallet.getRemainingCredits());
            }

            wallet.deductCredits(credits);
            wallet = walletRepository.save(wallet);

            recordTransaction(wallet, TransactionType.CONSUME, credits,
                    ReferenceType.ANALYSIS, referenceId, idempotencyKey,
                    "Analysis: " + credits + " credit(s) consumed");

            log.info("Consumed {} credits for userId={}, refId={}", credits, userId, referenceId);
            return toDto(wallet);
        });
    }

    @Override
    @Transactional // Not readOnly — lazily creates wallet on first access
    public void validateSufficientCredits(String userId, int requiredCredits) {
        var wallet = getOrCreateWallet(userId);
        if (wallet.getRemainingCredits() < requiredCredits) {
            throw new InsufficientCreditsException(requiredCredits, wallet.getRemainingCredits());
        }
    }

    // ---- Helpers ----

    private UserCreditWallet getOrCreateWallet(String userId) {
        String tenantId = resolveTenantId();
        return walletRepository.findByUserIdAndTenantId(userId, tenantId)
                .orElseGet(() -> createWallet(userId, tenantId));
    }

    private UserCreditWallet createWallet(String userId, String tenantId) {
        // Find trial plan to auto-grant free credits on first wallet creation
        var trialPlanOpt = planRepository.findByNameAndIsActiveTrue("trial");
        int trialCredits = trialPlanOpt.map(Plan::getCredits).orElse(0);

        var wallet = UserCreditWallet.builder()
                .userId(userId)
                .tenantId(tenantId)
                .totalCredits(trialCredits)
                .hasUsedTrial(trialCredits > 0)
                .build();
        wallet = walletRepository.save(wallet);

        // Record the trial grant transaction
        if (trialCredits > 0) {
            String idempotencyKey = "trial-" + userId;
            recordTransaction(wallet, TransactionType.GRANT, trialCredits,
                    ReferenceType.TRIAL, trialPlanOpt.get().getName(), idempotencyKey,
                    "Welcome! " + trialCredits + " free trial credits");
            log.info("Auto-granted {} trial credits to new user {}", trialCredits, userId);
        }

        return wallet;
    }

    private void recordTransaction(UserCreditWallet wallet, TransactionType type,
            int credits, ReferenceType referenceType,
            String referenceId, String idempotencyKey,
            String description) {
        var txn = CreditTransaction.builder()
                .userId(wallet.getUserId())
                .tenantId(wallet.getTenantId())
                .type(type)
                .credits(credits)
                .balanceAfter(wallet.getRemainingCredits())
                .referenceType(referenceType)
                .referenceId(referenceId)
                .idempotencyKey(idempotencyKey)
                .description(description)
                .build();
        transactionRepository.save(txn);
    }

    private String resolveTenantId() {
        String tenantId = TenantContext.getCurrentTenant();
        return (tenantId != null && !tenantId.isBlank()) ? tenantId : TenantContext.DEFAULT_TENANT;
    }

    private WalletDto toDto(UserCreditWallet wallet) {
        return WalletDto.builder()
                .total(wallet.getTotalCredits())
                .used(wallet.getConsumedCredits())
                .remaining(wallet.getRemainingCredits())
                .build();
    }

    /**
     * Execute a credit operation with optimistic lock retry.
     * On {@link ObjectOptimisticLockingFailureException}, retries up to MAX_RETRIES
     * times.
     */
    private WalletDto executeWithOptimisticRetry(java.util.function.Supplier<WalletDto> operation) {
        int attempts = 0;
        while (true) {
            try {
                return operation.get();
            } catch (ObjectOptimisticLockingFailureException e) {
                attempts++;
                if (attempts >= MAX_RETRIES) {
                    log.error("Optimistic lock failed after {} retries", MAX_RETRIES);
                    throw e;
                }
                log.warn("Optimistic lock conflict, retry {}/{}", attempts, MAX_RETRIES);
            }
        }
    }
}
