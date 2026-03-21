package com.learning.backendservice.service.export;

import com.learning.backendservice.domain.rule37.CalculationSummary;
import com.learning.backendservice.domain.rule37.InterestRow;
import com.learning.backendservice.domain.rule37.LedgerResult;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Rule37ExcelExportStrategyTest {

    private Rule37ExcelExportStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new Rule37ExcelExportStrategy();
    }

    @Test
    void supports_correctFormat_returnsTrue() {
        assertTrue(strategy.supports("excel", "issues"));
        assertTrue(strategy.supports("EXCEL", "COMPLETE"));
        assertFalse(strategy.supports("csv", "issues"));
        assertFalse(strategy.supports("excel", "gstr3b")); // Handled by another strategy
    }

    @Test
    void expectedFileExtensionAndContentType() {
        assertEquals("xlsx", strategy.getFileExtension());
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", strategy.getContentType());
    }

    @Test
    void generate_createsExcelWorkbook_withValidSheetsAndDisclaimer() throws Exception {
        InterestRow row = InterestRow.builder()
                .supplier("Test Supplier")
                .invoiceNumber("INV-001")
                .purchaseDate(LocalDate.of(2023, 1, 1))
                .paymentDate(LocalDate.of(2023, 2, 1))
                .principal(new BigDecimal("1000"))
                .itcAmount(new BigDecimal("180"))
                .delayDays(0)
                .interest(BigDecimal.ZERO)
                .status(InterestRow.InterestStatus.PAID_ON_TIME)
                .riskCategory(InterestRow.RiskCategory.SAFE)
                .build();

        CalculationSummary summary = CalculationSummary.builder()
                .totalInterest(BigDecimal.ZERO)
                .totalItcReversal(new BigDecimal("180"))
                .details(List.of(row))
                .build();

        LedgerResult result1 = new LedgerResult("Ledger A", summary);
        // Duplicate name to test deduplication
        LedgerResult result2 = new LedgerResult("Ledger A", summary);
        // Long name to test truncation
        LedgerResult result3 = new LedgerResult("This Is A Very Long Ledger Name That Exceeds Excel Limit", summary);

        byte[] bytes = strategy.generate(List.of(result1, result2, result3), "test", "complete");

        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        // Verify the workbook structure
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertEquals(4, workbook.getNumberOfSheets()); // Summary + 3 Ledgers

            Sheet summarySheet = workbook.getSheet("Summary");
            assertNotNull(summarySheet);
            
            // Check for the disclaimer in the summary sheet
            int lastRowNum = summarySheet.getLastRowNum();
            String lastRowContent = summarySheet.getRow(lastRowNum).getCell(0).getStringCellValue();
            assertTrue(lastRowContent.contains("Estimated calculations"));

            // Check deduplication
            Sheet sheetA1 = workbook.getSheet("Ledger A");
            Sheet sheetA2 = workbook.getSheet("Ledger A (2)");
            assertNotNull(sheetA1);
            assertNotNull(sheetA2);

            // Check truncation
            Sheet sheetLong = workbook.getSheet("This Is A Very Long Ledger Name"); // First 31 chars
            assertNotNull(sheetLong);
        }
    }
}
