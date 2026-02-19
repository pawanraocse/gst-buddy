package com.learning.authservice.credit.dto;

import lombok.Builder;

/**
 * Response DTO for user credit wallet balance.
 */
@Builder
public record WalletDto(
        int total,
        int used,
        int remaining) {
}
