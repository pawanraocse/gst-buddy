package com.learning.backendservice.service.export;

import com.learning.backendservice.domain.rule37.InterestRow;
import com.learning.backendservice.domain.rule37.LedgerResult;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class Gstr3bSummaryExportStrategy implements ExportStrategy {

    @Override
    public boolean supports(String format, String reportType) {
        return "excel".equalsIgnoreCase(format) && "gstr3b".equalsIgnoreCase(reportType);
    }

    @Override
    public byte[] generate(List<LedgerResult> ledgerResults, String filename) {
        return generate(ledgerResults, filename, "gstr3b");
    }

    @Override
    public byte[] generate(List<LedgerResult> ledgerResults, String filename, String reportType) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            // Extract all unpaid ITC reversals grouped by GSTR-3B Period
            Map<String, BigDecimal> reversalsByPeriod = ledgerResults.stream()
                    .flatMap(lr -> lr.getSummary().getDetails().stream())
                    .filter(r -> r.getStatus() == InterestRow.InterestStatus.UNPAID && r.getItcAmount().compareTo(BigDecimal.ZERO) > 0)
                    .collect(Collectors.groupingBy(
                            InterestRow::getGstr3bPeriod,
                            Collectors.reducing(BigDecimal.ZERO, InterestRow::getItcAmount, BigDecimal::add)
                    ));

            Sheet sheet = workbook.createSheet("GSTR-3B Table 4(B)(2)");
            
            // Header
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("GSTR-3B Return Period");
            header.createCell(1).setCellValue("Rule 37 ITC Reversal Amount");
            
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            header.getCell(0).setCellStyle(headerStyle);
            header.getCell(1).setCellStyle(headerStyle);

            CellStyle currencyStyle = workbook.createCellStyle();
            currencyStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));

            int rowNum = 1;
            BigDecimal total = BigDecimal.ZERO;
            
            for (Map.Entry<String, BigDecimal> entry : reversalsByPeriod.entrySet()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(entry.getKey());
                Cell amountCell = row.createCell(1);
                amountCell.setCellValue(entry.getValue().doubleValue());
                amountCell.setCellStyle(currencyStyle);
                total = total.add(entry.getValue());
            }

            // Total Row
            Row totalRow = sheet.createRow(rowNum);
            Cell totalLabel = totalRow.createCell(0);
            totalLabel.setCellValue("TOTAL REVERSAL REQUIRED");
            totalLabel.setCellStyle(headerStyle);
            
            Cell totalAmount = totalRow.createCell(1);
            totalAmount.setCellValue(total.doubleValue());
            CellStyle boldCurrency = workbook.createCellStyle();
            boldCurrency.cloneStyleFrom(currencyStyle);
            boldCurrency.setFont(font);
            totalAmount.setCellStyle(boldCurrency);

            sheet.setColumnWidth(0, 30 * 256);
            sheet.setColumnWidth(1, 40 * 256);

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate GSTR-3B export: " + e.getMessage(), e);
        }
    }

    @Override
    public String getContentType() {
        return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    }

    @Override
    public String getFileExtension() {
        return "xlsx";
    }
}
