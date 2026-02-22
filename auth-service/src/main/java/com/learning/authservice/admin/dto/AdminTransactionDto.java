package com.learning.authservice.admin.dto;

import lombok.Builder;

import java.time.Instant;

/**
 * Credit transaction record for admin view.
 */
@Builder
public record AdminTransactionDto(
        Long id,
        String userId,
        String type,
        int credits,
        int balanceAfter,
        String referenceType,
        String referenceId,
        String description,
        Instant createdAt) {
}
