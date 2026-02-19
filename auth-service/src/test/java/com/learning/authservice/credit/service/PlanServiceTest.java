package com.learning.authservice.credit.service;

import com.learning.authservice.credit.dto.PlanDto;
import com.learning.authservice.credit.entity.Plan;
import com.learning.authservice.credit.repository.PlanRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanServiceTest {

    @Mock
    private PlanRepository planRepository;
    @InjectMocks
    private PlanService planService;

    @Test
    @DisplayName("getActivePlans returns sorted plan DTOs")
    void getActivePlans() {
        var trial = Plan.builder().id(1L).name("trial").displayName("Trial")
                .priceInr(BigDecimal.ZERO).credits(2).isTrial(true).description("Free starter").build();
        var pro = Plan.builder().id(2L).name("pro").displayName("Pro")
                .priceInr(BigDecimal.valueOf(1000)).credits(5).isTrial(false).description("5 analyses").build();

        when(planRepository.findByIsActiveTrueOrderBySortOrderAsc())
                .thenReturn(List.of(trial, pro));

        List<PlanDto> plans = planService.getActivePlans();

        assertThat(plans).hasSize(2);
        assertThat(plans.get(0).name()).isEqualTo("trial");
        assertThat(plans.get(0).isTrial()).isTrue();
        assertThat(plans.get(0).credits()).isEqualTo(2);
        assertThat(plans.get(1).name()).isEqualTo("pro");
        assertThat(plans.get(1).priceInr()).isEqualByComparingTo(BigDecimal.valueOf(1000));
    }

    @Test
    @DisplayName("getActivePlanByName returns plan entity")
    void getActivePlanByName() {
        var ultra = Plan.builder().id(3L).name("ultra").displayName("Ultra")
                .priceInr(BigDecimal.valueOf(3000)).credits(30).isTrial(false).build();

        when(planRepository.findByNameAndIsActiveTrue("ultra"))
                .thenReturn(Optional.of(ultra));

        Plan plan = planService.getActivePlanByName("ultra");

        assertThat(plan.getName()).isEqualTo("ultra");
        assertThat(plan.getCredits()).isEqualTo(30);
    }

    @Test
    @DisplayName("getActivePlanByName throws if plan not found")
    void throwsForMissingPlan() {
        when(planRepository.findByNameAndIsActiveTrue("nonexistent"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> planService.getActivePlanByName("nonexistent"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Plan not found");
    }

    @Test
    @DisplayName("getActivePlans returns empty list when no plans")
    void returnsEmptyWhenNoPlans() {
        when(planRepository.findByIsActiveTrueOrderBySortOrderAsc())
                .thenReturn(List.of());

        List<PlanDto> plans = planService.getActivePlans();

        assertThat(plans).isEmpty();
    }
}
