package com.learning.backendservice.controller;

import com.learning.backendservice.entity.Rule37CalculationRun;
import com.learning.backendservice.service.Rule37CalculationRunService;
import com.learning.backendservice.service.export.ExportStrategy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rule37/runs")
@RequiredArgsConstructor
@Tag(name = "Rule 37 Export", description = "Export Rule 37 calculation runs to Excel")
public class Rule37ExportController {

    private final Rule37CalculationRunService runService;
    private final ExportStrategy exportStrategy;

    @Operation(summary = "Export run to Excel", description = "Download calculation run as Excel file")
    @ApiResponse(responseCode = "200", description = "Excel file")
    @ApiResponse(responseCode = "404", description = "Run not found")
    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> exportRun(
            @Parameter(description = "Run ID") @PathVariable Long id,
            @RequestParam(value = "format", defaultValue = "excel") String format) {
        Rule37CalculationRun run = runService.getRunEntity(id);
        byte[] bytes = exportStrategy.generate(run.getCalculationData(), run.getFilename());
        String safeFilename = sanitizeFilename(run.getFilename())
                + "_Interest_Calculation." + exportStrategy.getFileExtension();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(exportStrategy.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        org.springframework.http.ContentDisposition.attachment()
                                .filename(safeFilename)
                                .build()
                                .toString())
                .body(bytes);
    }

    private static String sanitizeFilename(String raw) {
        if (raw == null || raw.isBlank()) {
            return "export";
        }
        return raw.replaceAll("[^a-zA-Z0-9._\\- ]", "_").replaceAll("_{2,}", "_");
    }
}
