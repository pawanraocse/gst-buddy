package com.learning.authservice.credit.exception;

/**
 * Thrown when a user attempts an operation requiring more credits than
 * available.
 * Maps to HTTP 402 Payment Required.
 */
public class InsufficientCreditsException extends RuntimeException {

    private final int required;
    private final int available;

    public InsufficientCreditsException(int required, int available) {
        super("Insufficient credits: required=" + required + ", available=" + available);
        this.required = required;
        this.available = available;
    }

    public int getRequired() {
        return required;
    }

    public int getAvailable() {
        return available;
    }
}
