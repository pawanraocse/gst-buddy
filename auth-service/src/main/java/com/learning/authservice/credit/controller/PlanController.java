package com.learning.authservice.credit.controller;

import com.learning.authservice.credit.dto.PlanDto;
import com.learning.authservice.credit.service.PlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public API for retrieving pricing plans.
 * No authentication required — plans are displayed on the landing page.
 */
@RestController
@RequestMapping("/api/v1/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    /**
     * Returns all active pricing plans for display.
     */
    @GetMapping
    public ResponseEntity<List<PlanDto>> getActivePlans() {
        return ResponseEntity.ok(planService.getActivePlans());
    }
}
