package com.learning.authservice.credit.repository;

import com.learning.authservice.credit.entity.UserCreditWallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for {@link UserCreditWallet} entities.
 * Uses optimistic locking via {@code @Version} on the entity.
 */
@Repository
public interface UserCreditWalletRepository extends JpaRepository<UserCreditWallet, Long> {

    /**
     * Find wallet by user and tenant. Uses optimistic locking via entity's @Version
     * field.
     */
    Optional<UserCreditWallet> findByUserIdAndTenantId(String userId, String tenantId);

    /**
     * Find wallet with pessimistic write lock for critical operations.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<UserCreditWallet> findWithLockByUserIdAndTenantId(String userId, String tenantId);
}
