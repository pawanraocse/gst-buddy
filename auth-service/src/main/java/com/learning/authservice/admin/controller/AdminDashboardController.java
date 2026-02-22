package com.learning.authservice.admin.controller;

import com.learning.authservice.admin.dto.AdminDashboardStatsDto;
import com.learning.authservice.admin.service.AdminCreditService;
import com.learning.authservice.security.entity.Role;
import com.learning.authservice.security.repository.RoleRepository;
import com.learning.common.infra.security.RequirePermission;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Platform admin dashboard with aggregate stats and reference data.
 */
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminCreditService adminCreditService;
    private final RoleRepository roleRepository;

    @GetMapping("/stats")
    @RequirePermission(resource = "admin", action = "dashboard")
    public ResponseEntity<AdminDashboardStatsDto> getStats() {
        return ResponseEntity.ok(adminCreditService.getDashboardStats());
    }

    @GetMapping("/roles")
    @RequirePermission(resource = "admin", action = "dashboard")
    public ResponseEntity<List<Map<String, String>>> getAvailableRoles() {
        List<Map<String, String>> roles = roleRepository.findAll().stream()
                .map(r -> Map.of(
                        "id", r.getId(),
                        "name", r.getName(),
                        "scope", r.getScope()))
                .toList();
        return ResponseEntity.ok(roles);
    }
}
