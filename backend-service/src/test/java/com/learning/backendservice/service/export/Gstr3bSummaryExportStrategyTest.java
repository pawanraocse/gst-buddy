package com.learning.backendservice.service.export;

import com.learning.backendservice.domain.rule37.CalculationSummary;
import com.learning.backendservice.domain.rule37.InterestRow;
import com.learning.backendservice.domain.rule37.LedgerResult;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Gstr3bSummaryExportStrategyTest {

    private Gstr3bSummaryExportStrategy exporter;

    @BeforeEach
    void setUp() {
        exporter = new Gstr3bSummaryExportStrategy();
    }

    @Test
    @DisplayName("Strategy supports exactly 'excel' format and 'gstr3b' reportType")
    void testSupports() {
        assertTrue(exporter.supports("excel", "gstr3b"));
        assertTrue(exporter.supports("EXCEL", "GSTR3B"));
        assertTrue(!exporter.supports("pdf", "gstr3b"));
        assertTrue(!exporter.supports("excel", "issues"));
    }

    @Test
    @DisplayName("Generates correct aggregations by GSTR-3B Period")
    void testGenerateAggregations() throws Exception {
        InterestRow r1 = InterestRow.builder()
                .supplier("Supplier A")
                .status(InterestRow.InterestStatus.UNPAID)
                .itcAmount(new BigDecimal("100.00"))
                .interest(BigDecimal.ZERO)
                .gstr3bPeriod("Feb 2025")
                .riskCategory(InterestRow.RiskCategory.BREACHED)
                .build();
        InterestRow r2 = InterestRow.builder()
                .supplier("Supplier B")
                .status(InterestRow.InterestStatus.UNPAID)
                .itcAmount(new BigDecimal("200.00"))
                .interest(BigDecimal.ZERO)
                .gstr3bPeriod("Feb 2025")
                .riskCategory(InterestRow.RiskCategory.BREACHED)
                .build();
        InterestRow r3 = InterestRow.builder()
                .supplier("Supplier C")
                .status(InterestRow.InterestStatus.UNPAID)
                .itcAmount(new BigDecimal("150.00"))
                .interest(BigDecimal.ZERO)
                .gstr3bPeriod("Mar 2025")
                .riskCategory(InterestRow.RiskCategory.BREACHED)
                .build();
        // Paid on time shouldn't be included
        InterestRow r4 = InterestRow.builder()
                .supplier("Supplier D")
                .status(InterestRow.InterestStatus.PAID_ON_TIME)
                .itcAmount(new BigDecimal("400.00"))
                .gstr3bPeriod("Apr 2025")
                .build();

        CalculationSummary summary = CalculationSummary.builder()
                .calculationDate(LocalDate.now())
                .details(List.of(r1, r2, r3, r4))
                .build();

        LedgerResult ledgerResult = new LedgerResult("Test Ledger", summary);

        byte[] bytes = exporter.generate(List.of(ledgerResult), "test", "gstr3b");
        
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheet("GSTR-3B Table 4(B)(2)");
            
            // Row 0 is header
            
            // Collect rows 1 and 2 into a map
            java.util.Map<String, Double> results = new java.util.HashMap<>();
            results.put(sheet.getRow(1).getCell(0).getStringCellValue(), sheet.getRow(1).getCell(1).getNumericCellValue());
            results.put(sheet.getRow(2).getCell(0).getStringCellValue(), sheet.getRow(2).getCell(1).getNumericCellValue());

            assertEquals(150.0, results.get("Mar 2025"), 0.001);
            assertEquals(300.0, results.get("Feb 2025"), 0.001);

            Row totalRow = sheet.getRow(3);
            assertEquals("TOTAL REVERSAL REQUIRED", totalRow.getCell(0).getStringCellValue());
            assertEquals(450.0, totalRow.getCell(1).getNumericCellValue(), 0.001); // 150 + 300
        }
    }
}
