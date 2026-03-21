package com.learning.backendservice.controller;

import com.learning.backendservice.BaseControllerTest;
import com.learning.backendservice.dto.UploadResult;
import com.learning.backendservice.service.Rule37CalculationRunService;
import com.learning.common.constants.HeaderNames;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LedgerUploadControllerTest extends BaseControllerTest {

    @MockBean
    private Rule37CalculationRunService runService;

    @Test
    void uploadLedgers_validFiles_returns201() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "valid.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "dummy content".getBytes()
        );

        UploadResult mockResult = UploadResult.builder()
                .runId(100L)
                .build();

        when(runService.processUpload(any(List.class), eq(LocalDate.parse("2024-03-31")), eq("user123")))
                .thenReturn(mockResult);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/ledgers/upload")
                        .file(file)
                        .param("asOnDate", "2024-03-31")
                        .header(HeaderNames.USER_ID, "user123")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.runId").value(100L));
    }

    @Test
    void uploadLedgers_invalidExtension_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "invalid.csv",
                "text/csv",
                "dummy content".getBytes()
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/ledgers/upload")
                        .file(file)
                        .param("asOnDate", "2024-03-31")
                        .header(HeaderNames.USER_ID, "user123")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid file type: 'invalid.csv'. Only .xlsx and .xls files are accepted."));
    }

    @Test
    void uploadLedgers_missingUserId_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "valid.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "dummy content".getBytes()
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/ledgers/upload")
                        .file(file)
                        .param("asOnDate", "2024-03-31")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Missing required header: X-User-Id. User identification is required for credit consumption."));
    }
}
