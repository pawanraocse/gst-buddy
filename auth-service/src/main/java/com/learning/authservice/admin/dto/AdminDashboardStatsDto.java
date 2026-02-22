package com.learning.authservice.admin.dto;

import lombok.Builder;

import java.math.BigDecimal;

/**
 * Aggregate statistics for the platform admin dashboard.
 */
@Builder
public record AdminDashboardStatsDto(
        long totalUsers,
        long activeUsers,
        long disabledUsers,
        long invitedUsers,
        long totalCreditsGranted,
        long totalCreditsConsumed,
        BigDecimal totalRevenueInr,
        long activePlans,
        long totalTransactions) {
}
