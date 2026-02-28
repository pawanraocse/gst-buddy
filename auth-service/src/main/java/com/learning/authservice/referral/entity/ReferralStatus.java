package com.learning.authservice.referral.entity;

/**
 * Status of a referral record.
 */
public enum ReferralStatus {
    /** Code is active and available for use. */
    ACTIVE,
    /** A referee has signed up using this code. Credits granted. */
    CONVERTED,
    /** Code has expired (future use). */
    EXPIRED
}
