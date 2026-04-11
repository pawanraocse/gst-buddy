package com.learning.backendservice.controller;

import com.learning.backendservice.dto.AuditRunResponse;
import com.learning.backendservice.engine.AuditRuleRegistry;
import com.learning.backendservice.service.AuditRunService;
import com.learning.common.constants.HeaderNames;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for generic audit run management.
 *
 * <p>Replaces {@code Rule37RunController} with a rule-agnostic API
 * that works for all GST compliance modules.
 */
@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@Tag(name = "Audit Engine", description = "Manage audit runs and query available GST compliance rules")
public class AuditRunController {

    private final AuditRunService auditRunService;
    private final AuditRuleRegistry ruleRegistry;

    // ─── Rule Catalog ──────────────────────────────────────────────────────────

    @Operation(summary = "List available audit rules",
            description = "Returns all registered GST compliance rules available for audit")
    @ApiResponse(responseCode = "200", description = "Rules listed")
    @GetMapping("/rules")
    public ResponseEntity<List<Map<String, Object>>> listRules() {
        List<Map<String, Object>> catalog = ruleRegistry.getAllRules().stream()
                .map(rule -> Map.<String, Object>of(
                        "ruleId", rule.getRuleId(),
                        "name", rule.getName(),
                        "displayName", rule.getDisplayName(),
                        "description", rule.getDescription(),
                        "category", rule.getCategory(),
                        "legalBasis", rule.getLegalBasis(),
                        "creditCost", rule.getCreditsRequired()))
                .toList();
        return ResponseEntity.ok(catalog);
    }

    // ─── Audit Runs CRUD ───────────────────────────────────────────────────────

    @Operation(summary = "List audit runs",
            description = "List paginated audit runs for the current user. Optionally filter by ruleId.")
    @ApiResponse(responseCode = "200", description = "Runs listed")
    @GetMapping("/runs")
    public ResponseEntity<Page<AuditRunResponse>> listRuns(
            HttpServletRequest request,
            @Parameter(description = "Optional rule ID filter (e.g. RULE_37_ITC_REVERSAL)")
            @RequestParam(required = false) String ruleId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        String userId = request.getHeader(HeaderNames.USER_ID);
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(auditRunService.listRuns(userId, ruleId, pageable));
    }

    @Operation(summary = "Get audit run by ID",
            description = "Get full audit run including result data and findings summary")
    @ApiResponse(responseCode = "200", description = "Run found")
    @ApiResponse(responseCode = "404", description = "Run not found or belongs to another user")
    @GetMapping("/runs/{id}")
    public ResponseEntity<AuditRunResponse> getRun(
            HttpServletRequest request,
            @Parameter(description = "Audit run UUID v7") @PathVariable UUID id) {
        String userId = request.getHeader(HeaderNames.USER_ID);
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(auditRunService.getRun(id, userId));
    }

    @Operation(summary = "Delete audit run",
            description = "Permanently delete a completed audit run and all its findings")
    @ApiResponse(responseCode = "204", description = "Deleted")
    @ApiResponse(responseCode = "404", description = "Run not found or belongs to another user")
    @DeleteMapping("/runs/{id}")
    public ResponseEntity<Void> deleteRun(
            HttpServletRequest request,
            @Parameter(description = "Audit run UUID v7") @PathVariable UUID id) {
        String userId = request.getHeader(HeaderNames.USER_ID);
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        auditRunService.deleteRun(id, userId);
        return ResponseEntity.noContent().build();
    }
}
