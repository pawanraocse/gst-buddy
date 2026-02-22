package com.learning.authservice.admin.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.List;

/**
 * Rich user view for the admin panel — includes roles and wallet summary.
 */
@Builder
public record AdminUserDetailDto(
        String userId,
        String email,
        String name,
        String avatarUrl,
        String status,
        String source,
        String tenantId,
        List<String> roles,
        WalletSummaryDto wallet,
        Instant firstLoginAt,
        Instant lastLoginAt,
        Instant createdAt) {

    @Builder
    public record WalletSummaryDto(
            int total,
            int used,
            int remaining) {
    }
}
