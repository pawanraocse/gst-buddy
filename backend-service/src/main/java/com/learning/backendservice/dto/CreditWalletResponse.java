package com.learning.backendservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO from auth-service credit API.
 * Maps to the WalletDto returned by CreditController.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditWalletResponse {
    private int total;
    private int used;
    private int remaining;
}
