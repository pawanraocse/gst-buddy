package com.learning.authservice.credit.entity;

/**
 * Types of credit transactions in the audit ledger.
 */
public enum TransactionType {
    /** Credits added to wallet (purchase, trial, admin). */
    GRANT,
    /** Credits consumed during analysis. */
    CONSUME,
    /** Credits returned (failed analysis, admin refund). */
    REFUND,
    /** Manual adjustment by admin. */
    ADJUSTMENT
}
