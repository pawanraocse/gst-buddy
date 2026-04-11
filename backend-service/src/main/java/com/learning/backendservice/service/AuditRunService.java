package com.learning.backendservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.backendservice.dto.AuditRunResponse;
import com.learning.backendservice.entity.AuditRun;
import com.learning.backendservice.engine.AuditRule;
import com.learning.backendservice.engine.AuditRuleRegistry;
import com.learning.backendservice.repository.AuditRunRepository;
import com.learning.common.infra.exception.NotFoundException;
import com.learning.common.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * CRUD service for {@link AuditRun} entities.
 *
 * <p>All operations are automatically scoped to the current tenant via
 * {@link TenantContext}, enforcing row-level multi-tenant isolation.
 *
 * <p>This service handles <em>persistence only</em>. Business orchestration
 * (file processing, credits, S3 upload) lives in {@link AuditRunOrchestrator}.
 */
@Service
@RequiredArgsConstructor
public class AuditRunService {

    private final AuditRunRepository runRepository;
    private final AuditRuleRegistry ruleRegistry;
    private final ObjectMapper objectMapper;

    /**
     * List audit runs for the current tenant, paginated.
     * Optionally filtered by ruleId.
     */
    public Page<AuditRunResponse> listRuns(String userId, String ruleId, Pageable pageable) {
        String tenantId = TenantContext.getCurrentTenant();
        Page<AuditRun> page = (ruleId != null && !ruleId.isBlank())
                ? runRepository.findByTenantIdAndUserIdAndRuleId(tenantId, userId, ruleId, pageable)
                : runRepository.findByTenantIdAndUserId(tenantId, userId, pageable);
        return page.map(run -> toResponse(run, false));
    }

    /**
     * Get a single audit run by ID with full result data.
     *
     * @throws NotFoundException if the run doesn't exist or belongs to another tenant
     */
    public AuditRunResponse getRun(UUID id, String userId) {
        String tenantId = TenantContext.getCurrentTenant();
        return runRepository.findByIdAndTenantIdAndUserId(id, tenantId, userId)
                .map(run -> toResponse(run, true))
                .orElseThrow(() -> new NotFoundException("Audit run not found: " + id));
    }

    /**
     * Delete an audit run.
     *
     * @throws NotFoundException if the run doesn't exist or belongs to another tenant
     */
    @Transactional
    public void deleteRun(UUID id, String userId) {
        String tenantId = TenantContext.getCurrentTenant();
        if (!runRepository.existsByIdAndTenantIdAndUserId(id, tenantId, userId)) {
            throw new NotFoundException("Audit run not found: " + id);
        }
        runRepository.deleteById(id);
    }

    /**
     * Fetch the raw entity (for export use-cases where the service layer needs the full object).
     *
     * @throws NotFoundException if the run doesn't exist or belongs to another tenant
     */
    public AuditRun getRunEntity(UUID id) {
        String tenantId = TenantContext.getCurrentTenant();
        return runRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Audit run not found: " + id));
    }

    // ─── Mapping ─────────────────────────────────────────────────────────────

    private AuditRunResponse toResponse(AuditRun run, boolean includeResultData) {
        String displayName = null;
        try {
            AuditRule<?, ?> rule = ruleRegistry.getRule(run.getRuleId());
            displayName = rule.getDisplayName();
        } catch (IllegalArgumentException ex) {
            displayName = run.getRuleId(); // fallback for unknown/deleted rules
        }

        return AuditRunResponse.builder()
                .id(run.getId().toString())
                .ruleId(run.getRuleId())
                .ruleDisplayName(displayName)
                .status(run.getStatus())
                .totalImpactAmount(run.getTotalImpactAmount())
                .creditsConsumed(run.getCreditsConsumed())
                .createdAt(run.getCreatedAt())
                .completedAt(run.getCompletedAt())
                .expiresAt(run.getExpiresAt())
                .userId(run.getUserId())
                .inputMetadata(fromJson(run.getInputMetadata()))
                .resultData(includeResultData ? fromJson(run.getResultData()) : null)
                .build();
    }

    private Object fromJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (JsonProcessingException e) {
            return json; // fallback: return raw string
        }
    }
}
