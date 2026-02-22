package com.learning.authservice.credit.service;

import com.learning.authservice.credit.dto.PlanDto;
import com.learning.authservice.credit.entity.Plan;
import com.learning.authservice.credit.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for retrieving pricing plans.
 */
@Service
@RequiredArgsConstructor
public class PlanService {

    private final PlanRepository planRepository;

    /**
     * Returns all active plans, ordered for display.
     */
    @Transactional(readOnly = true)
    public List<PlanDto> getActivePlans() {
        return planRepository.findByIsActiveTrueOrderBySortOrderAsc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Find an active plan by name.
     */
    @Transactional(readOnly = true)
    public Plan getActivePlanByName(String name) {
        return planRepository.findByNameAndIsActiveTrue(name)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + name));
    }

    private PlanDto toDto(Plan plan) {
        return PlanDto.builder()
                .id(plan.getId())
                .name(plan.getName())
                .displayName(plan.getDisplayName())
                .priceInr(plan.getPriceInr())
                .credits(plan.getCredits())
                .isTrial(plan.getIsTrial())
                .isActive(plan.getIsActive())
                .description(plan.getDescription())
                .validityDays(plan.getValidityDays())
                .sortOrder(plan.getSortOrder() != null ? plan.getSortOrder() : 0)
                .build();
    }
}
