package com.learning.backendservice.exception;

/**
 * Thrown when a user doesn't have enough credits for analysis.
 * Maps to HTTP 402 Payment Required.
 */
public class InsufficientCreditsException extends RuntimeException {

    public InsufficientCreditsException(String message) {
        super(message);
    }
}
