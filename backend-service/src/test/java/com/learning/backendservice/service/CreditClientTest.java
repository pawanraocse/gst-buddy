package com.learning.backendservice.service;

import com.learning.backendservice.dto.CreditWalletResponse;
import com.learning.backendservice.exception.InsufficientCreditsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"rawtypes", "unchecked"})
class CreditClientTest {

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private CreditClient creditClient;

    @BeforeEach
    void setUp() {
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        creditClient = new CreditClient(webClientBuilder);
    }

    @Test
    void consumeCredits_success() {
        CreditWalletResponse mockResponse = new CreditWalletResponse(10, 1, 9);
        
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/api/v1/credits/consume")).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(CreditWalletResponse.class)).thenReturn(Mono.just(mockResponse));

        CreditWalletResponse response = creditClient.consumeCredits("user123", 1, "ref-456", "idemp-789");

        assertEquals(10, response.getTotal());
        assertEquals(1, response.getUsed());
        assertEquals(9, response.getRemaining());
    }

    @Test
    void consumeCredits_insufficientCredits_throwsException() {
        WebClientResponseException mockException = WebClientResponseException.create(
                402, "Payment Required", null, "Not enough credits".getBytes(), null);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/api/v1/credits/consume")).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(CreditWalletResponse.class)).thenReturn(Mono.error(mockException));

        InsufficientCreditsException exception = assertThrows(InsufficientCreditsException.class, () -> {
            creditClient.consumeCredits("user123", 5, "ref-456", "idemp-789");
        });

        assertEquals("Insufficient credits: Not enough credits", exception.getMessage());
    }

    @Test
    void consumeCredits_serverError_throwsRuntimeException() {
        WebClientResponseException mockException = WebClientResponseException.create(
                500, "Internal Server Error", null, "Boom".getBytes(), null);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/api/v1/credits/consume")).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(CreditWalletResponse.class)).thenReturn(Mono.error(mockException));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            creditClient.consumeCredits("user123", 1, "ref-456", "idemp-789");
        });

        assertTrue(exception.getMessage().contains("Credit service error: 500 Internal Server Error"));
    }

    @Test
    void checkBalance_success() {
        CreditWalletResponse mockResponse = new CreditWalletResponse(10, 2, 8);
        
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/api/v1/credits")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(CreditWalletResponse.class)).thenReturn(Mono.just(mockResponse));

        CreditWalletResponse response = creditClient.checkBalance("user123", 5);

        assertEquals(8, response.getRemaining());
    }

    @Test
    void checkBalance_insufficient_throwsException() {
        CreditWalletResponse mockResponse = new CreditWalletResponse(10, 8, 2);
        
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/api/v1/credits")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(CreditWalletResponse.class)).thenReturn(Mono.just(mockResponse));

        InsufficientCreditsException exception = assertThrows(InsufficientCreditsException.class, () -> {
            creditClient.checkBalance("user123", 5);
        });

        assertEquals("Insufficient credits: need 5 but only 2 available", exception.getMessage());
    }

    @Test
    void getWallet_success() {
        CreditWalletResponse mockResponse = new CreditWalletResponse(100, 50, 50);
        
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/api/v1/credits")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(CreditWalletResponse.class)).thenReturn(Mono.just(mockResponse));

        CreditWalletResponse response = creditClient.getWallet("user123");

        assertEquals(100, response.getTotal());
        assertEquals(50, response.getUsed());
        assertEquals(50, response.getRemaining());
    }
}
