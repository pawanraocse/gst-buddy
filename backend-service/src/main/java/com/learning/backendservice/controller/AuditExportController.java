package com.learning.backendservice.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.backendservice.domain.rule37.LedgerResult;
import com.learning.backendservice.entity.AuditRun;
import com.learning.backendservice.service.AuditRunService;
import com.learning.backendservice.service.export.ExportStrategy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles Excel export for audit runs.
 *
 * <p>Export URL: {@code GET /api/v1/audit/runs/{id}/export}
 *
 * <p>Currently supports Rule 37 result data (LedgerResult[]).
 * Future rules can add new ExportStrategy implementations without
 * changing this controller.
 */
@RestController
@RequestMapping("/api/v1/audit/runs")
@RequiredArgsConstructor
@Tag(name = "Audit Export", description = "Export audit run results to Excel/PDF")
public class AuditExportController {

    private final AuditRunService auditRunService;
    private final List<ExportStrategy> exportStrategies;
    private final ObjectMapper objectMapper;

    @Operation(summary = "Export audit run", description = "Download audit run results as Excel file")
    @ApiResponse(responseCode = "200", description = "Excel file returned")
    @ApiResponse(responseCode = "404", description = "Run not found")
    @ApiResponse(responseCode = "400", description = "Unsupported format or report type")
    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> exportRun(
            @Parameter(description = "Audit run UUID v7") @PathVariable UUID id,
            @RequestParam(value = "format", defaultValue = "excel") String format,
            @Parameter(description = "Report type: 'issues' (default), 'complete', or 'gstr3b'")
            @RequestParam(value = "reportType", defaultValue = "issues") String reportType) {

        AuditRun run = auditRunService.getRunEntity(id);

        ExportStrategy strategy = exportStrategies.stream()
                .filter(s -> s.supports(format, reportType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unsupported export format/reportType: " + format + "/" + reportType));

        // Extract LedgerResult list from JSONB result_data
        // This is Rule 37-specific; future rules will dispatch based on ruleId
        List<LedgerResult> ledgerResults = extractLedgerResults(run);

        // Extract filename from input_metadata
        String filename = extractFilename(run);

        byte[] bytes = strategy.generate(ledgerResults, filename, reportType);
        String safeFilename = sanitizeFilename(filename)
                + "_" + (reportType.equalsIgnoreCase("gstr3b") ? "GSTR3B_Summary" : "Interest_Calculation")
                + "." + strategy.getFileExtension();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(strategy.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(safeFilename).build().toString())
                .body(bytes);
    }

    // ─── Private Helpers ────────────────────────────────────────────────────

    private List<LedgerResult> extractLedgerResults(AuditRun run) {
        try {
            String json = run.getResultData();
            return objectMapper.readValue(json, new TypeReference<List<LedgerResult>>() {});
        } catch (Exception e) {
            String primaryRuleId = (run.getRulesExecuted() != null && run.getRulesExecuted().length > 0)
                    ? run.getRulesExecuted()[0] : "UNKNOWN";
            throw new IllegalStateException(
                    "Cannot export run " + run.getId() + ": result_data is not a valid LedgerResult list (ruleId=" + primaryRuleId + ")", e);
        }
    }

    private String extractFilename(AuditRun run) {
        try {
            String json = run.getInputMetadata();
            if (json != null && !json.isBlank()) {
                Map<String, Object> meta = objectMapper.readValue(json, new TypeReference<>() {});
                Object f = meta.get("filename");
                if (f instanceof String s) return s;
            }
        } catch (Exception ignored) {}
        return "audit-run-" + run.getId();
    }

    private static String sanitizeFilename(String raw) {
        if (raw == null || raw.isBlank()) return "export";
        return raw.replaceAll("[^a-zA-Z0-9._\\- ]", "_").replaceAll("_{2,}", "_");
    }
}
