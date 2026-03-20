package com.learning.backendservice.service.export;

import com.learning.backendservice.domain.rule37.CalculationSummary;
import com.learning.backendservice.domain.rule37.InterestRow;
import com.learning.backendservice.domain.rule37.LedgerResult;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Exports Rule 37 calculation results to Excel.
 * All numeric columns use proper Excel number types so formulas (SUM, AVERAGE,
 * etc.) work.
 */
@Component
public class Rule37ExcelExportStrategy implements ExportStrategy {

    private static final int MAX_SHEET_NAME_LENGTH = 31;

    @Override
    public boolean supports(String format, String reportType) {
        return "excel".equalsIgnoreCase(format) && !"gstr3b".equalsIgnoreCase(reportType);
    }

    @Override
    public byte[] generate(List<LedgerResult> ledgerResults, String filename) {
        return generate(ledgerResults, filename, "issues");
    }

    @Override
    public byte[] generate(List<LedgerResult> ledgerResults, String filename, String reportType) {
        boolean issuesOnly = !"complete".equalsIgnoreCase(reportType);
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // ── Reusable cell styles ──
            var styles = createStyles(workbook);

            // ── Summary sheet ──
            Sheet summarySheet = workbook.createSheet("Summary");
            writeSummarySheet(summarySheet, ledgerResults, issuesOnly, styles);

            // ── Per-ledger detail sheets ──
            for (LedgerResult lr : ledgerResults) {
                String sheetName = sanitizeSheetName(lr.getLedgerName());
                Sheet sheet = workbook.createSheet(sheetName);
                writeLedgerSheet(sheet, lr.getSummary(), issuesOnly, styles);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Excel export: " + e.getMessage(), e);
        }
    }

    // ══════════════════════════════════════════════════════
    // Summary Sheet
    // ══════════════════════════════════════════════════════

    private void writeSummarySheet(Sheet sheet, List<LedgerResult> ledgerResults,
            boolean issuesOnly, Styles styles) {
        int rowNum = 0;

        // Header
        Row header = sheet.createRow(rowNum++);
        header.createCell(0).setCellValue("Ledger Name");
        header.createCell(1).setCellValue("Total ITC Reversal");
        header.createCell(2).setCellValue("Total Interest");
        header.createCell(3).setCellValue("Report Type");
        applyHeaderStyle(header, styles.headerStyle, 4);

        // Data rows
        for (LedgerResult lr : ledgerResults) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(lr.getLedgerName());
            setCurrencyCell(row, 1, lr.getSummary().getTotalItcReversal(), styles.currencyStyle);
            setCurrencyCell(row, 2, lr.getSummary().getTotalInterest(), styles.currencyStyle);
            row.createCell(3).setCellValue(issuesOnly ? "Issues Only" : "Complete");
        }

        // Grand Total
        rowNum++;
        Row totalRow = sheet.createRow(rowNum);
        Cell totalLabel = totalRow.createCell(0);
        totalLabel.setCellValue("GRAND TOTAL");
        totalLabel.setCellStyle(styles.headerStyle);

        BigDecimal totalItc = ledgerResults.stream()
                .map(lr -> lr.getSummary().getTotalItcReversal())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalInterest = ledgerResults.stream()
                .map(lr -> lr.getSummary().getTotalInterest())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        setCurrencyCell(totalRow, 1, totalItc, styles.currencyBoldStyle);
        setCurrencyCell(totalRow, 2, totalInterest, styles.currencyBoldStyle);

        sheet.setColumnWidth(0, 40 * 256);
        sheet.setColumnWidth(1, 20 * 256);
        sheet.setColumnWidth(2, 20 * 256);
        sheet.setColumnWidth(3, 15 * 256);
    }

    // ══════════════════════════════════════════════════════
    // Ledger Detail Sheet
    // ══════════════════════════════════════════════════════

    private void writeLedgerSheet(Sheet sheet, CalculationSummary summary,
            boolean issuesOnly, Styles styles) {
        List<InterestRow> rows = summary.getDetails();
        if (issuesOnly) {
            rows = rows.stream()
                    .filter(r -> r.getStatus() != InterestRow.InterestStatus.PAID_ON_TIME
                            && !(r.getRiskCategory() == InterestRow.RiskCategory.SAFE
                                    && r.getInterest().signum() == 0))
                    .collect(Collectors.toList());
        }

        int rowNum = 0;

        // Header
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = { "Supplier", "Invoice/Voucher No.", "Purchase Date", "Payment Date",
                "Ledger Amount (Incl. GST)", "Delay Days", "ITC (18%)", "ITC to Reverse",
                "Interest (18% p.a.)", "Status", "Payment Deadline", "Risk Category", "GSTR-3B Period" };
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(styles.headerStyle);
        }

        // Data rows — proper types for every column
        for (InterestRow r : rows) {
            Row row = sheet.createRow(rowNum++);

            // Col 0: Supplier (text)
            row.createCell(0).setCellValue(r.getSupplier());

            // Col 1: Invoice/Voucher No. (text)
            row.createCell(1).setCellValue(
                    r.getInvoiceNumber() != null ? r.getInvoiceNumber() : "");

            // Col 2: Purchase Date (date)
            setDateCell(row, 2, r.getPurchaseDate(), styles.dateStyle);

            // Col 3: Payment Date (date or "Unpaid")
            if (r.getPaymentDate() != null) {
                setDateCell(row, 3, r.getPaymentDate(), styles.dateStyle);
            } else {
                row.createCell(3).setCellValue("Unpaid");
            }

            // Col 4: Ledger Amount (number)
            setCurrencyCell(row, 4, r.getPrincipal(), styles.currencyStyle);

            // Col 5: Delay Days (integer)
            setIntCell(row, 5, r.getDelayDays(), styles.integerStyle);

            // Col 6: ITC (number)
            setCurrencyCell(row, 6, r.getItcAmount(), styles.currencyStyle);

            // Col 7: ITC to Reverse (number only for UNPAID with BREACHED risk)
            if (r.getStatus() == InterestRow.InterestStatus.UNPAID
                    && r.getRiskCategory() == InterestRow.RiskCategory.BREACHED) {
                setCurrencyCell(row, 7, r.getItcAmount(), styles.currencyStyle);
            } else if (r.getStatus() == InterestRow.InterestStatus.UNPAID
                    && r.getRiskCategory() == InterestRow.RiskCategory.AT_RISK) {
                row.createCell(7).setCellValue("Potential");
            }
            // else leave cell empty (no value)

            // Col 8: Interest (number)
            setCurrencyCell(row, 8, r.getInterest(), styles.currencyStyle);

            // Col 9: Status (text)
            row.createCell(9).setCellValue(formatStatus(r.getStatus()));

            // Col 10: Payment Deadline (date)
            setDateCell(row, 10, r.getPaymentDeadline(), styles.dateStyle);

            // Col 11: Risk Category (text)
            row.createCell(11).setCellValue(
                    r.getRiskCategory() != null ? r.getRiskCategory().name() : "");

            // Col 12: GSTR-3B Period (text)
            row.createCell(12).setCellValue(
                    r.getGstr3bPeriod() != null ? r.getGstr3bPeriod() : "");
        }

        // Total row
        rowNum += 2;
        Row totalRow = sheet.createRow(rowNum);
        Cell totalLabel = totalRow.createCell(0);
        totalLabel.setCellValue("TOTAL");
        totalLabel.setCellStyle(styles.headerStyle);
        setCurrencyCell(totalRow, 7, summary.getTotalItcReversal(), styles.currencyBoldStyle);
        setCurrencyCell(totalRow, 8, summary.getTotalInterest(), styles.currencyBoldStyle);

        // Column widths
        int[] widths = { 30, 20, 15, 15, 20, 12, 18, 18, 20, 12, 15, 15, 15 };
        for (int i = 0; i < widths.length; i++) {
            sheet.setColumnWidth(i, widths[i] * 256);
        }
    }

    // ══════════════════════════════════════════════════════
    // Cell Helpers — write proper Excel types
    // ══════════════════════════════════════════════════════

    private static void setCurrencyCell(Row row, int col, Object value, CellStyle style) {
        Cell cell = row.createCell(col);
        if (value instanceof Number n) {
            cell.setCellValue(n.doubleValue());
        } else if (value instanceof String s) {
            try {
                cell.setCellValue(Double.parseDouble(s));
            } catch (NumberFormatException e) {
                cell.setCellValue(0.0);
            }
        } else {
            cell.setCellValue(0.0);
        }
        cell.setCellStyle(style);
    }

    private static void setDateCell(Row row, int col, LocalDate date, CellStyle style) {
        if (date == null) {
            row.createCell(col).setCellValue("N/A");
            return;
        }
        Cell cell = row.createCell(col);
        cell.setCellValue(Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        cell.setCellStyle(style);
    }

    private static void setIntCell(Row row, int col, int value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private static void applyHeaderStyle(Row row, CellStyle style, int colCount) {
        for (int i = 0; i < colCount; i++) {
            Cell cell = row.getCell(i);
            if (cell != null)
                cell.setCellStyle(style);
        }
    }

    // ══════════════════════════════════════════════════════
    // Styles
    // ══════════════════════════════════════════════════════

    private record Styles(CellStyle headerStyle, CellStyle currencyStyle,
            CellStyle currencyBoldStyle, CellStyle dateStyle, CellStyle integerStyle) {
    }

    private static Styles createStyles(Workbook workbook) {
        DataFormat dataFormat = workbook.createDataFormat();

        // Bold font for headers and totals
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);

        // Header style
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(boldFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        // Currency: ₹ #,##0.00
        CellStyle currencyStyle = workbook.createCellStyle();
        currencyStyle.setDataFormat(dataFormat.getFormat("#,##0.00"));

        // Currency bold (for totals)
        CellStyle currencyBoldStyle = workbook.createCellStyle();
        currencyBoldStyle.setDataFormat(dataFormat.getFormat("#,##0.00"));
        currencyBoldStyle.setFont(boldFont);

        // Date: dd/MM/yyyy
        CellStyle dateStyle = workbook.createCellStyle();
        dateStyle.setDataFormat(dataFormat.getFormat("dd/MM/yyyy"));

        // Integer (delay days)
        CellStyle integerStyle = workbook.createCellStyle();
        integerStyle.setDataFormat(dataFormat.getFormat("0"));

        return new Styles(headerStyle, currencyStyle, currencyBoldStyle, dateStyle, integerStyle);
    }

    // ══════════════════════════════════════════════════════
    // Text Helpers
    // ══════════════════════════════════════════════════════

    private static String formatStatus(InterestRow.InterestStatus status) {
        if (status == null)
            return "";
        return switch (status) {
            case PAID_LATE -> "Paid Late";
            case PAID_ON_TIME -> "Paid on Time";
            case UNPAID -> "Unpaid";
        };
    }

    private static String sanitizeSheetName(String name) {
        if (name == null || name.isEmpty())
            return "Sheet";
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
