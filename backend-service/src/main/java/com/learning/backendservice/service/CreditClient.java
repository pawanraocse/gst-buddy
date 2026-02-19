package com.learning.backendservice.service;

import com.learning.backendservice.dto.CreditWalletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * WebClient wrapper for calling auth-service credit APIs.
 * Uses Eureka service discovery (lb://AUTH-SERVICE).
 */
@Component
@Slf4j
public class CreditClient {

    private final WebClient webClient;

    public CreditClient(@Qualifier("internalWebClientBuilder") WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("lb://AUTH-SERVICE/auth")
                .build();
    }

    /**
     * Consume credits for analysis. Called after ledger parsing succeeds.
     *
     * @param userId         the user performing analysis
     * @param credits        number of credits to consume
     * @param referenceId    analysis run reference
     * @param idempotencyKey unique key to prevent double-deduction
     * @return wallet balance after consumption
     * @throws InsufficientCreditsException (propagated as WebClientResponseException.ClientError 402)
     */
    public CreditWalletResponse consumeCredits(String userId, int credits, String referenceId, String idempotencyKey) {
        log.info("Requesting credit consumption: userId={}, credits={}, idempotencyKey={}", userId, credits, idempotencyKey);

        try {
            return webClient.post()
                    .uri("/api/v1/credits/consume")
                    .bodyValue(new ConsumeRequest(userId, credits, referenceId, idempotencyKey))
                    .retrieve()
                    .bodyToMono(CreditWalletResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == 402) {
                log.warn("Insufficient credits for userId={}: {}", userId, e.getResponseBodyAsString());
                throw new com.learning.backendservice.exception.InsufficientCreditsException(
                        "Insufficient credits: " + e.getResponseBodyAsString());
            }
            log.error("Credit consumption failed: userId={}, status={}, error={}", userId, e.getStatusCode(), e.getMessage());
            throw new RuntimeException("Credit service error: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Credit consumption failed: userId={}, error={}", userId, e.getMessage());
            throw new RuntimeException("Credit service unavailable: " + e.getMessage(), e);
        }
    }

    /**
     * Get wallet balance for a user.
     */
    public CreditWalletResponse getWallet(String userId) {
        return webClient.get()
                .uri("/api/v1/credits")
                .header("X-User-Id", userId)
                .retrieve()
                .bodyToMono(CreditWalletResponse.class)
                .block();
    }

    private record ConsumeRequest(String userId, int credits, String referenceId, String idempotencyKey) {}
}
