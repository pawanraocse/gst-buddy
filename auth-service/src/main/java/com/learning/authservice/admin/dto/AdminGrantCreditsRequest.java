package com.learning.authservice.admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for admin credit grant/revoke.
 */
public record AdminGrantCreditsRequest(
        @NotNull @Min(1) Integer credits,
        String description) {
}
