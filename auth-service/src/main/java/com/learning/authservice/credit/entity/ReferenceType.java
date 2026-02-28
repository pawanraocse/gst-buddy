package com.learning.authservice.credit.entity;

/**
 * Describes what triggered a credit transaction.
 * Extensible for future use cases (subscriptions, promo codes, etc.).
 */
public enum ReferenceType {
    /** Trial credits granted on signup. */
    TRIAL,
    /** Credits from purchasing a plan. */
    PLAN_PURCHASE,
    /** Credits consumed during ledger analysis. */
    ANALYSIS,
    /** Credits granted by admin (incentive, support). */
    ADMIN_GRANT,
    /** Credits granted via the referral system (both referrer and referee). */
    REFERRAL,
    /** Promotional credits (campaigns, generic promos). */
    PROMO,
    /** Credits returned after failed analysis or admin action. */
    REFUND
}
