package com.learning.authservice.credit.repository;

import com.learning.authservice.credit.entity.CreditTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link CreditTransaction} entities.
 * Transactions are immutable — only inserts, no updates or deletes.
 */
@Repository
public interface CreditTransactionRepository extends JpaRepository<CreditTransaction, Long> {

    /**
     * Check if a transaction with the given idempotency key already exists.
     * Used to prevent duplicate credit operations (e.g. double-charging).
     */
    boolean existsByIdempotencyKey(String idempotencyKey);

    /**
     * Transaction history for a user, most recent first.
     */
    List<CreditTransaction> findByUserIdOrderByCreatedAtDesc(String userId);
}
