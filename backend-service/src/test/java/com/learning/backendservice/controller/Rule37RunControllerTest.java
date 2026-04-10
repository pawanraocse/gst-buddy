package com.learning.backendservice.controller;

import com.learning.backendservice.BaseControllerTest;
import com.learning.backendservice.dto.Rule37RunResponse;
import com.learning.backendservice.service.Rule37CalculationRunService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class Rule37RunControllerTest extends BaseControllerTest {

    private static final String TEST_USER_ID = "user-123";

    @MockBean
    private Rule37CalculationRunService runService;

    @Test
    void listRuns_success() throws Exception {
        Rule37RunResponse runResponse = Rule37RunResponse.builder()
                .id(1L)
                .filename("test.xlsx")
                .totalItcReversal(new BigDecimal("100.00"))
                .totalInterest(new BigDecimal("50.00"))
                .createdAt(OffsetDateTime.parse("2024-03-21T10:00:00Z"))
                .build();

        Page<Rule37RunResponse> page = new PageImpl<>(List.of(runResponse), PageRequest.of(0, 10), 1);
        when(runService.listRuns(eq(TEST_USER_ID), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/rule37/runs")
                        .header("X-User-Id", TEST_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].filename").value("test.xlsx"))
                .andExpect(jsonPath("$.content[0].totalItcReversal").value(100.0))
                .andExpect(jsonPath("$.content[0].totalInterest").value(50.0));
    }

    @Test
    void listRuns_missingUserId_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/rule37/runs")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void getRun_found() throws Exception {
        Rule37RunResponse runResponse = Rule37RunResponse.builder()
                .id(2L)
                .filename("details.xlsx")
                .build();

        when(runService.getRun(eq(2L), eq(TEST_USER_ID))).thenReturn(runResponse);

        mockMvc.perform(get("/api/v1/rule37/runs/2")
                        .header("X-User-Id", TEST_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.filename").value("details.xlsx"));
    }

    @Test
    void deleteRun_success() throws Exception {
        doNothing().when(runService).deleteRun(eq(3L), eq(TEST_USER_ID));

        mockMvc.perform(delete("/api/v1/rule37/runs/3")
                        .header("X-User-Id", TEST_USER_ID))
                .andExpect(status().isNoContent());
    }
}
