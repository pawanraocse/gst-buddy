package com.learning.authservice.admin.controller;

import com.learning.authservice.admin.dto.CreatePlanRequest;
import com.learning.authservice.admin.dto.UpdatePlanRequest;
import com.learning.authservice.credit.dto.PlanDto;
import com.learning.authservice.credit.entity.Plan;
import com.learning.authservice.credit.repository.PlanRepository;
import com.learning.common.infra.exception.NotFoundException;
import com.learning.common.infra.security.RequirePermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * CRUD for pricing plans (admin only).
 */
@RestController
@RequestMapping("/api/v1/admin/plans")
@RequiredArgsConstructor
@Slf4j
public class AdminPlanController {

    private final PlanRepository planRepository;

    @GetMapping
    @RequirePermission(resource = "plan", action = "manage")
    public ResponseEntity<List<PlanDto>> listAllPlans() {
        List<PlanDto> plans = planRepository.findAll().stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(plans);
    }

    @PostMapping
    @RequirePermission(resource = "plan", action = "manage")
    public ResponseEntity<PlanDto> createPlan(@Valid @RequestBody CreatePlanRequest req) {
        Plan plan = Plan.builder()
                .name(req.name())
                .displayName(req.displayName())
                .priceInr(req.priceInr())
                .credits(req.credits())
                .isTrial(req.isTrial())
                .description(req.description())
                .validityDays(req.validityDays())
                .sortOrder(req.sortOrder() != null ? req.sortOrder() : 0)
                .build();
        plan = planRepository.save(plan);
        log.info("Admin created plan: {}", plan.getName());
        return ResponseEntity
                .created(URI.create("/api/v1/admin/plans/" + plan.getId()))
                .body(toDto(plan));
    }

    @PutMapping("/{planId}")
    @RequirePermission(resource = "plan", action = "manage")
    public ResponseEntity<PlanDto> updatePlan(
            @PathVariable Long planId,
            @RequestBody UpdatePlanRequest req) {

        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new NotFoundException("Plan not found: " + planId));

        if (req.displayName() != null) plan.setDisplayName(req.displayName());
        if (req.priceInr() != null) plan.setPriceInr(req.priceInr());
        if (req.credits() != null) plan.setCredits(req.credits());
        if (req.isTrial() != null) plan.setIsTrial(req.isTrial());
        if (req.isActive() != null) plan.setIsActive(req.isActive());
        if (req.description() != null) plan.setDescription(req.description());
        if (req.validityDays() != null) plan.setValidityDays(req.validityDays());
        if (req.sortOrder() != null) plan.setSortOrder(req.sortOrder());

        plan = planRepository.save(plan);
        log.info("Admin updated plan: {} (id={})", plan.getName(), planId);
        return ResponseEntity.ok(toDto(plan));
    }

    @PatchMapping("/{planId}/toggle")
    @RequirePermission(resource = "plan", action = "manage")
    public ResponseEntity<PlanDto> togglePlan(@PathVariable Long planId) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new NotFoundException("Plan not found: " + planId));
        plan.setIsActive(!plan.getIsActive());
        plan = planRepository.save(plan);
        log.info("Admin toggled plan: {} -> active={} (id={})", plan.getName(), plan.getIsActive(), planId);
        return ResponseEntity.ok(toDto(plan));
    }

    @DeleteMapping("/{planId}")
    @RequirePermission(resource = "plan", action = "manage")
    public ResponseEntity<Void> deactivatePlan(@PathVariable Long planId) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new NotFoundException("Plan not found: " + planId));
        plan.setIsActive(false);
        planRepository.save(plan);
        log.info("Admin deactivated plan: {} (id={})", plan.getName(), planId);
        return ResponseEntity.noContent().build();
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
