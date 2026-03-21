package com.learning.backendservice.service.export;

import com.learning.backendservice.domain.rule37.InterestRow;
import com.learning.backendservice.domain.rule37.LedgerResult;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Component
public class Gstr3bSummaryExportStrategy implements ExportStrategy {

    /** Formatter for period strings like "Apr 2025", "Jun 2025". */
    private static final DateTimeFormatter PERIOD_FORMAT =
            DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

    /** Comparator that sorts period strings chronologically. */
    private static final Comparator<String> PERIOD_COMPARATOR = (a, b) -> {
        try {
            YearMonth ya = YearMonth.parse(a, PERIOD_FORMAT);
            YearMonth yb = YearMonth.parse(b, PERIOD_FORMAT);
            return ya.compareTo(yb);
        } catch (DateTimeParseException e) {
            return a.compareTo(b); // fallback: lexicographic
        }
    };

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
            
            // Filter: only UNPAID + BREACHED invoices with actual ITC to reverse
            var reversalRows = ledgerResults.stream()
                    .flatMap(lr -> lr.getSummary().getDetails().stream())
                    .filter(r -> r.getStatus() == InterestRow.InterestStatus.UNPAID
                            && r.getRiskCategory() == InterestRow.RiskCategory.BREACHED
                            && r.getItcAmount().compareTo(BigDecimal.ZERO) > 0)
                    .toList();

            // Group reversals by GSTR-3B period — sorted chronologically
            Map<String, BigDecimal> reversalsByPeriod = reversalRows.stream()
                    .collect(Collectors.groupingBy(
                            InterestRow::getGstr3bPeriod,
                            () -> new TreeMap<>(PERIOD_COMPARATOR),
                            Collectors.reducing(BigDecimal.ZERO, InterestRow::getItcAmount, BigDecimal::add)
                    ));

            // Group interest by GSTR-3B period
            Map<String, BigDecimal> interestByPeriod = reversalRows.stream()
                    .collect(Collectors.groupingBy(
                            InterestRow::getGstr3bPeriod,
                            Collectors.reducing(BigDecimal.ZERO, InterestRow::getInterest, BigDecimal::add)
                    ));

            Sheet sheet = workbook.createSheet("GSTR-3B Table 4(B)(2)");
            
            // Header
            Row header = sheet.createRow(0);
            String[] headers = {
                "GSTR-3B Return Period",
                "ITC Reversal — Table 4(B)(2)",
                "Interest Payable — Sec. 50(1)",
                "Total Liability"
            };
            
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            CellStyle currencyStyle = workbook.createCellStyle();
            currencyStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));

            int rowNum = 1;
            BigDecimal totalReversal = BigDecimal.ZERO;
            BigDecimal totalInterest = BigDecimal.ZERO;
            
            for (Map.Entry<String, BigDecimal> entry : reversalsByPeriod.entrySet()) {
                String period = entry.getKey();
                BigDecimal reversal = entry.getValue();
                BigDecimal interest = interestByPeriod.getOrDefault(period, BigDecimal.ZERO);
                BigDecimal liability = reversal.add(interest);

                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(period);

                Cell reversalCell = row.createCell(1);
                reversalCell.setCellValue(reversal.doubleValue());
                reversalCell.setCellStyle(currencyStyle);

                Cell interestCell = row.createCell(2);
                interestCell.setCellValue(interest.doubleValue());
                interestCell.setCellStyle(currencyStyle);

                Cell liabilityCell = row.createCell(3);
                liabilityCell.setCellValue(liability.doubleValue());
                liabilityCell.setCellStyle(currencyStyle);

                totalReversal = totalReversal.add(reversal);
                totalInterest = totalInterest.add(interest);
            }

            // Total Row
            CellStyle boldCurrency = workbook.createCellStyle();
            boldCurrency.cloneStyleFrom(currencyStyle);
            boldCurrency.setFont(font);

            Row totalRow = sheet.createRow(rowNum);
            Cell totalLabel = totalRow.createCell(0);
            totalLabel.setCellValue("TOTAL REVERSAL REQUIRED");
            totalLabel.setCellStyle(headerStyle);

            Cell totalRevCell = totalRow.createCell(1);
            totalRevCell.setCellValue(totalReversal.doubleValue());
            totalRevCell.setCellStyle(boldCurrency);

            Cell totalIntCell = totalRow.createCell(2);
            totalIntCell.setCellValue(totalInterest.doubleValue());
            totalIntCell.setCellStyle(boldCurrency);

            Cell totalLiabCell = totalRow.createCell(3);
            totalLiabCell.setCellValue(totalReversal.add(totalInterest).doubleValue());
            totalLiabCell.setCellStyle(boldCurrency);

            // Footer note — legal reference
            rowNum += 2;
            Row noteRow = sheet.createRow(rowNum);
            noteRow.createCell(0).setCellValue(
                    "Note: Interest payable u/s 50(1) CGST Act on ITC wrongly availed & utilised. "
                    + "Report reversal in GSTR-3B Table 4(B)(2), reclaim in 4(A)(5) upon payment. "
                    + "Interest in Table 5.1 of GSTR-3B.");

            sheet.setColumnWidth(0, 30 * 256);
            sheet.setColumnWidth(1, 35 * 256);
            sheet.setColumnWidth(2, 35 * 256);
            sheet.setColumnWidth(3, 25 * 256);

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
