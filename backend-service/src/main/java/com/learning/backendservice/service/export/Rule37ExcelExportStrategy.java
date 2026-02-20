package com.learning.backendservice.service.export;

import com.learning.backendservice.domain.rule37.CalculationSummary;
import com.learning.backendservice.domain.rule37.InterestRow;
import com.learning.backendservice.domain.rule37.LedgerResult;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Exports Rule 37 calculation results to Excel.
 * Port of MVP {@code excelExport.ts}.
 */
@Component
public class Rule37ExcelExportStrategy implements ExportStrategy {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int MAX_SHEET_NAME_LENGTH = 31;

    @Override
    public byte[] generate(List<LedgerResult> ledgerResults, String filename) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // Summary sheet first
            Sheet summarySheet = workbook.createSheet("Summary");
            int rowNum = 0;
            summarySheet.createRow(rowNum++).createCell(0).setCellValue("Ledger Name");
            summarySheet.getRow(0).createCell(1).setCellValue("Total ITC Reversal");
            summarySheet.getRow(0).createCell(2).setCellValue("Total Interest");
            for (LedgerResult lr : ledgerResults) {
                Row row = summarySheet.createRow(rowNum++);
                row.createCell(0).setCellValue(lr.getLedgerName());
                row.createCell(1).setCellValue(formatCurrency(lr.getSummary().getTotalItcReversal()));
                row.createCell(2).setCellValue(formatCurrency(lr.getSummary().getTotalInterest()));
            }
            rowNum++;
            Row totalRow = summarySheet.createRow(rowNum);
            totalRow.createCell(0).setCellValue("GRAND TOTAL");
            double totalItc = ledgerResults.stream().mapToDouble(lr -> lr.getSummary().getTotalItcReversal()).sum();
            double totalInterest = ledgerResults.stream().mapToDouble(lr -> lr.getSummary().getTotalInterest()).sum();
            totalRow.createCell(1).setCellValue(formatCurrency(totalItc));
            totalRow.createCell(2).setCellValue(formatCurrency(totalInterest));

            summarySheet.setColumnWidth(0, 40 * 256);
            summarySheet.setColumnWidth(1, 20 * 256);
            summarySheet.setColumnWidth(2, 20 * 256);

            // Per-ledger sheets
            for (LedgerResult lr : ledgerResults) {
                String sheetName = sanitizeSheetName(lr.getLedgerName());
                Sheet sheet = workbook.createSheet(sheetName);
                writeLedgerSheet(sheet, lr.getSummary());
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Excel export: " + e.getMessage(), e);
        }
    }

    private void writeLedgerSheet(Sheet sheet, CalculationSummary summary) {
        int rowNum = 0;
        Row headerRow = sheet.createRow(rowNum++);
        headerRow.createCell(0).setCellValue("Supplier");
        headerRow.createCell(1).setCellValue("Purchase Date");
        headerRow.createCell(2).setCellValue("Payment Date");
        headerRow.createCell(3).setCellValue("Principal Amount");
        headerRow.createCell(4).setCellValue("Delay Days");
        headerRow.createCell(5).setCellValue("ITC Amount (18%)");
        headerRow.createCell(6).setCellValue("Interest (18% p.a.)");
        headerRow.createCell(7).setCellValue("Status");
        headerRow.createCell(8).setCellValue("Payment Deadline");
        headerRow.createCell(9).setCellValue("Risk Category");
        headerRow.createCell(10).setCellValue("GSTR-3B Period");

        for (InterestRow r : summary.getDetails()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(r.getSupplier());
            row.createCell(1).setCellValue(formatDate(r.getPurchaseDate()));
            row.createCell(2).setCellValue(r.getPaymentDate() != null ? formatDate(r.getPaymentDate()) : "Unpaid");
            row.createCell(3).setCellValue(formatCurrency(r.getPrincipal()));
            row.createCell(4).setCellValue(r.getDelayDays());
            row.createCell(5).setCellValue(formatCurrency(r.getItcAmount()));
            row.createCell(6).setCellValue(formatCurrency(r.getInterest()));
            row.createCell(7).setCellValue(r.getStatus() == InterestRow.InterestStatus.PAID_LATE ? "Paid Late" : "Unpaid");
            row.createCell(8).setCellValue(formatDate(r.getPaymentDeadline()));
            row.createCell(9).setCellValue(r.getRiskCategory() != null ? r.getRiskCategory().name() : "");
            row.createCell(10).setCellValue(r.getGstr3bPeriod() != null ? r.getGstr3bPeriod() : "");
        }

        rowNum++;
        rowNum++;
        Row totalRow = sheet.createRow(rowNum);
        totalRow.createCell(0).setCellValue("TOTAL");
        totalRow.createCell(5).setCellValue(formatCurrency(summary.getTotalItcReversal()));
        totalRow.createCell(6).setCellValue(formatCurrency(summary.getTotalInterest()));

        sheet.setColumnWidth(0, 30 * 256);
        sheet.setColumnWidth(1, 15 * 256);
        sheet.setColumnWidth(2, 15 * 256);
        sheet.setColumnWidth(3, 18 * 256);
        sheet.setColumnWidth(4, 12 * 256);
        sheet.setColumnWidth(5, 18 * 256);
        sheet.setColumnWidth(6, 20 * 256);
        sheet.setColumnWidth(7, 12 * 256);
        sheet.setColumnWidth(8, 15 * 256);
        sheet.setColumnWidth(9, 15 * 256);
        sheet.setColumnWidth(10, 15 * 256);
    }

    private static String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FORMAT) : "N/A";
    }

    private static String formatCurrency(double amount) {
        return String.format("%.2f", amount);
    }

    private static String sanitizeSheetName(String name) {
        if (name == null || name.isEmpty()) return "Sheet";
        String sanitized = name.replaceAll("[:\\\\/?*\\[\\]]", "_");
        return sanitized.length() > MAX_SHEET_NAME_LENGTH
                ? sanitized.substring(0, MAX_SHEET_NAME_LENGTH)
                : sanitized;
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
