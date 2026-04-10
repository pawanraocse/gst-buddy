package com.learning.backendservice.controller;

import com.learning.backendservice.dto.Rule37RunResponse;
import com.learning.backendservice.service.Rule37CalculationRunService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.learning.common.constants.HeaderNames;

@RestController
@RequestMapping("/api/v1/rule37/runs")
@RequiredArgsConstructor
@Tag(name = "Rule 37 Runs", description = "List, get, and delete Rule 37 calculation runs")
public class Rule37RunController {

    private final Rule37CalculationRunService runService;

    @Operation(summary = "List runs", description = "List calculation runs for the current user")
    @ApiResponse(responseCode = "200", description = "List retrieved")
    @GetMapping
    public ResponseEntity<Page<Rule37RunResponse>> listRuns(
            HttpServletRequest request,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        String userId = request.getHeader(HeaderNames.USER_ID);
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(runService.listRuns(userId, pageable));
    }

    @Operation(summary = "Get run by ID", description = "Get full calculation run including calculation data")
    @ApiResponse(responseCode = "200", description = "Run found")
    @ApiResponse(responseCode = "404", description = "Run not found")
    @GetMapping("/{id}")
    public ResponseEntity<Rule37RunResponse> getRun(
            HttpServletRequest request,
            @Parameter(description = "Run ID") @PathVariable Long id) {
        String userId = request.getHeader(HeaderNames.USER_ID);
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(runService.getRun(id, userId));
    }

    @Operation(summary = "Delete run", description = "Delete a calculation run")
    @ApiResponse(responseCode = "204", description = "Deleted")
    @ApiResponse(responseCode = "404", description = "Run not found")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRun(
            HttpServletRequest request,
            @Parameter(description = "Run ID") @PathVariable Long id) {
        String userId = request.getHeader(HeaderNames.USER_ID);
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        runService.deleteRun(id, userId);
        return ResponseEntity.noContent().build();
    }
}
