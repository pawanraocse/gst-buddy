package com.learning.backendservice.controller;

import com.learning.backendservice.BaseControllerTest;
import com.learning.backendservice.dto.AuditRunResponse;
import com.learning.backendservice.engine.AuditRuleRegistry;
import com.learning.backendservice.service.AuditRunService;
import com.learning.backendservice.util.UuidV7;
import com.learning.common.constants.HeaderNames;
import com.learning.common.infra.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("AuditRunController Integration")
class AuditRunControllerTest extends BaseControllerTest {

    @MockitoBean
    private AuditRunService auditRunService;

    @MockitoBean
    private AuditRuleRegistry auditRuleRegistry;

    @Test
    @DisplayName("GET /runs should return list for user")
    void shouldReturnRunsList() throws Exception {
        UUID runId = UuidV7.generate();
        AuditRunResponse response = AuditRunResponse.builder()
                .id(runId.toString())
                .ruleId("RULE_A")
                .status("COMPLETED")
                .createdAt(OffsetDateTime.now())
                .build();

        when(auditRunService.listRuns(any(), any(), any())).thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/audit/runs")
                        .header(HeaderNames.USER_ID, "user123")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(runId.toString()))
                .andExpect(jsonPath("$.content[0].ruleId").value("RULE_A"));
    }

    @Test
    @DisplayName("GET /runs/{id} should return specific run")
    void shouldReturnSpecificRun() throws Exception {
        UUID runId = UuidV7.generate();
        AuditRunResponse response = AuditRunResponse.builder()
                .id(runId.toString())
                .ruleId("RULE_A")
                .status("COMPLETED")
                .resultData(Map.of("key", "value"))
                .build();

        when(auditRunService.getRun(eq(runId), any())).thenReturn(response);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/audit/runs/" + runId)
                        .header(HeaderNames.USER_ID, "user123")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(runId.toString()))
                .andExpect(jsonPath("$.resultData.key").value("value"));
    }

    @Test
    @DisplayName("GET /runs/{id} should return 404 for unknown run")
    void shouldReturn404ForUnknownRun() throws Exception {
        UUID runId = UuidV7.generate();
        when(auditRunService.getRun(eq(runId), any())).thenThrow(new NotFoundException("Run not found"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/audit/runs/" + runId)
                        .header(HeaderNames.USER_ID, "user123")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
