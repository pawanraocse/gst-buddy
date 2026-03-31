package com.learning.authservice.credit.service;

import com.learning.authservice.credit.config.RazorpayConfig;
import com.learning.authservice.credit.entity.Plan;
import com.razorpay.Order;
import com.razorpay.OrderClient;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RazorpayPaymentServiceTest {

    @Mock
    private RazorpayConfig config;

    @Mock
    private PlanService planService;

    @Mock
    private RazorpayClient razorpayClient;

    @Mock
    private OrderClient orderClient;

    @InjectMocks
    private RazorpayPaymentService paymentService;

    @BeforeEach
    void setUp() {
        // Inject the mocked razorpayClient into the service
        // Since it's initialized in @PostConstruct, we use ReflectionTestUtils to override it
        ReflectionTestUtils.setField(paymentService, "razorpayClient", razorpayClient);
        // RazorpayClient uses public fields for its clients
        razorpayClient.orders = orderClient;
    }

    @Test
    void createOrder_shouldGenerateReceiptShorterThan40Chars() throws RazorpayException {
        // Arrange
        String planName = "pro";
        String userId = UUID.randomUUID().toString(); // 36 chars
        Plan mockPlan = new Plan();
        mockPlan.setName(planName);
        mockPlan.setPriceInr(BigDecimal.valueOf(999));
        mockPlan.setIsTrial(false);
        mockPlan.setCredits(100);

        when(planService.getActivePlanByName(planName)).thenReturn(mockPlan);

        Order mockOrder = mock(Order.class);
        when(mockOrder.get("id")).thenReturn("order_test_123");
        when(mockOrder.get("status")).thenReturn("created");
        when(orderClient.create(any(JSONObject.class))).thenReturn(mockOrder);

        // Act
        paymentService.createOrder(planName, userId);

        // Assert
        ArgumentCaptor<JSONObject> captor = ArgumentCaptor.forClass(JSONObject.class);
        verify(orderClient).create(captor.capture());

        JSONObject capturedPayload = captor.getValue();
        String receipt = capturedPayload.getString("receipt");

        System.out.println("Generated Receipt: " + receipt);
        System.out.println("Receipt Length: " + receipt.length());

        assertTrue(receipt.length() <= 40, "Receipt length should be <= 40, but was " + receipt.length());
        assertTrue(receipt.startsWith("p_"), "Receipt should start with 'p_'");
        assertEquals(99900, capturedPayload.getInt("amount"), "Amount should be in paise (999 * 100)");
    }

    @Test
    void createOrder_shouldFailForTrialPlan() {
        // Arrange
        String planName = "trial";
        String userId = UUID.randomUUID().toString();
        Plan mockPlan = new Plan();
        mockPlan.setIsTrial(true);

        when(planService.getActivePlanByName(planName)).thenReturn(mockPlan);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> paymentService.createOrder(planName, userId));
    }
}
