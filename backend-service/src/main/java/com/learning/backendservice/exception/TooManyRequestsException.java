package com.learning.backendservice.exception;

/**
 * Thrown when the server is at upload capacity (all semaphore permits consumed).
 * Maps to HTTP 429 Too Many Requests via GlobalExceptionHandler.
 */
public class TooManyRequestsException extends RuntimeException {

    public TooManyRequestsException(String message) {
        super(message);
    }
}
