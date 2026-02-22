package com.learning.backendservice.service;

import com.learning.backendservice.dto.Rule37RunResponse;
import com.learning.backendservice.dto.UploadResult;
import com.learning.backendservice.entity.Rule37CalculationRun;
import com.learning.backendservice.repository.Rule37RunRepository;
import com.learning.common.infra.exception.NotFoundException;
import com.learning.common.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class Rule37CalculationRunService {

    private final LedgerUploadOrchestrator ledgerUploadOrchestrator;
    private final Rule37RunRepository runRepository;

    public UploadResult processUpload(List<MultipartFile> files, java.time.LocalDate asOnDate, String createdBy) {
        return ledgerUploadOrchestrator.processUpload(files, asOnDate, createdBy);
    }

    public Page<Rule37RunResponse> listRuns(Pageable pageable) {
        String tenantId = TenantContext.getCurrentTenant();
        return runRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable)
                .map(this::toResponse);
    }

    public Rule37RunResponse getRun(Long id) {
        String tenantId = TenantContext.getCurrentTenant();
        return runRepository.findByIdAndTenantId(id, tenantId)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("Rule37 run not found: " + id));
    }

    @Transactional
    public void deleteRun(Long id) {
        String tenantId = TenantContext.getCurrentTenant();
        if (!runRepository.existsByIdAndTenantId(id, tenantId)) {
            throw new NotFoundException("Rule37 run not found: " + id);
        }
        runRepository.deleteById(id);
    }

    public Rule37CalculationRun getRunEntity(Long id) {
        String tenantId = TenantContext.getCurrentTenant();
        return runRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Rule37 run not found: " + id));
    }

    private Rule37RunResponse toResponse(Rule37CalculationRun run) {
        return Rule37RunResponse.builder()
                .id(run.getId())
                .filename(run.getFilename())
                .asOnDate(run.getAsOnDate())
                .totalInterest(run.getTotalInterest())
                .totalItcReversal(run.getTotalItcReversal())
                .createdAt(run.getCreatedAt())
                .createdBy(run.getCreatedBy())
                .expiresAt(run.getExpiresAt())
                .calculationData(run.getCalculationData())
                .build();
    }
}
