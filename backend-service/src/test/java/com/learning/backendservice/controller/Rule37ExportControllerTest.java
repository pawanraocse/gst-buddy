package com.learning.backendservice.controller;

import com.learning.backendservice.BaseControllerTest;
import com.learning.backendservice.entity.Rule37CalculationRun;
import com.learning.backendservice.service.Rule37CalculationRunService;
import com.learning.backendservice.service.export.ExportStrategy;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

class Rule37ExportControllerTest extends BaseControllerTest {

    private static final String TEST_USER_ID = "user-123";

    @MockBean
    private Rule37CalculationRunService runService;

    @MockBean(name = "rule37ExcelExportStrategy")
    private ExportStrategy excelExportStrategy;

    @MockBean(name = "gstr3bSummaryExportStrategy")
    private ExportStrategy gstr3bExportStrategy;

    @Test
    void exportRun_unsupportedFormat_returns400() throws Exception {
        Rule37CalculationRun run = Rule37CalculationRun.builder()
                .id(1L)
                .filename("test.xlsx")
                .build();

        when(runService.getRunEntity(1L, TEST_USER_ID)).thenReturn(run);
        when(excelExportStrategy.supports("pdf", "issues")).thenReturn(false);
        when(gstr3bExportStrategy.supports("pdf", "issues")).thenReturn(false);

        mockMvc.perform(get("/api/v1/rule37/runs/1/export?format=pdf&reportType=issues")
                        .header("X-User-Id", TEST_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unsupported export format or report type"));
    }

    @Test
    void exportRun_excelFormat_returnsFile() throws Exception {
        Rule37CalculationRun run = Rule37CalculationRun.builder()
                .id(2L)
                .filename("details.xlsx")
                .build();

        byte[] fakeExcelContent = "fake excel bytes".getBytes();

        when(runService.getRunEntity(2L, TEST_USER_ID)).thenReturn(run);
        when(excelExportStrategy.supports("excel", "issues")).thenReturn(true);
        when(excelExportStrategy.generate(run.getCalculationData(), "details.xlsx", "issues")).thenReturn(fakeExcelContent);
        when(excelExportStrategy.getFileExtension()).thenReturn("xlsx");
        when(excelExportStrategy.getContentType()).thenReturn("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        mockMvc.perform(get("/api/v1/rule37/runs/2/export?format=excel&reportType=issues")
                        .header("X-User-Id", TEST_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"details.xlsx_Interest_Calculation.xlsx\""))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(content().bytes(fakeExcelContent));
    }

    @Test
    void exportRun_cleansFilename() throws Exception {
        Rule37CalculationRun run = Rule37CalculationRun.builder()
                .id(3L)
                .filename("weird#file/name.xlsx")
                .build();

        byte[] fakeExcelContent = "fake excel bytes".getBytes();

        when(runService.getRunEntity(3L, TEST_USER_ID)).thenReturn(run);
        when(excelExportStrategy.supports("excel", "issues")).thenReturn(true);
        when(excelExportStrategy.generate(run.getCalculationData(), "weird#file/name.xlsx", "issues")).thenReturn(fakeExcelContent);
        when(excelExportStrategy.getFileExtension()).thenReturn("xlsx");
        when(excelExportStrategy.getContentType()).thenReturn("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        mockMvc.perform(get("/api/v1/rule37/runs/3/export?format=excel&reportType=issues")
                        .header("X-User-Id", TEST_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"weird_file_name.xlsx_Interest_Calculation.xlsx\""))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(content().bytes(fakeExcelContent));
    }
}
