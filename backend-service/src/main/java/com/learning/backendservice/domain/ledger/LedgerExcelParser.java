package com.learning.backendservice.domain.ledger;

import com.learning.backendservice.exception.LedgerParseException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Parses Tally/Busy ledger Excel files into LedgerEntry list.
 * Port of MVP {@code excelParser.ts}.
 *
 * <p>Column mapping (header-based): date, debit/dr, credit/cr, supplier/party/ledger/name.
 * Fallback: 4 columns with no credit header → position-based [Date, Debit, Credit, Supplier].
 */
@Component
public class LedgerExcelParser implements LedgerParser {

    private static final Logger log = LoggerFactory.getLogger(LedgerExcelParser.class);

    private static final long MAX_DECOMPRESSED_SIZE = 100L * 1024 * 1024; // 100 MB
    private static final int MAX_ROWS = 50_000;

    @Override
    public List<LedgerEntry> parse(InputStream inputStream, String filename) {
        String defaultSupplier = getFileNameWithoutExtension(filename);

        try (Workbook workbook = createWorkbookWithLimits(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) {
                throw new LedgerParseException("Excel file is empty");
            }
            validateRowCount(sheet);

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new LedgerParseException("Excel file has no header row");
            }

            int colCount = headerRow.getLastCellNum();
            List<String> headers = new ArrayList<>();
            List<String> normalizedHeaders = new ArrayList<>();
            for (int i = 0; i < colCount; i++) {
                Cell cell = headerRow.getCell(i);
                String h = cell != null ? getCellStringValue(cell) : "";
                headers.add(h);
                normalizedHeaders.add(normalizeColumnName(h));
            }

            int dateIndex = findIndex(normalizedHeaders, h -> h.contains("date"));
            int debitIndex = findIndex(normalizedHeaders, h -> h.contains("debit") || h.contains("dr"));
            int creditIndex = findIndex(normalizedHeaders, h -> h.contains("credit") || h.contains("cr"));
            int supplierIndex = findIndex(normalizedHeaders, h ->
                    h.contains("supplier") || h.contains("party") || h.contains("ledger") || h.contains("name"));

            // Position-based fallback: 4 columns, no credit header
            if (colCount == 4 && creditIndex == -1) {
                return parsePositionBased(sheet, defaultSupplier);
            }

            if (dateIndex == -1) {
                throw new LedgerParseException("Could not find Date column. Found headers: " + String.join(", ", headers));
            }
            if (debitIndex == -1 && creditIndex == -1) {
                throw new LedgerParseException("Could not find Debit or Credit columns. Found headers: " + String.join(", ", headers));
            }

            return parseHeaderBased(sheet, headers, dateIndex, debitIndex, creditIndex, supplierIndex, defaultSupplier);

        } catch (LedgerParseException e) {
            throw e;
        } catch (Exception e) {
            throw new LedgerParseException("Failed to parse Excel file: " + e.getMessage(), e);
        }
    }

    private Workbook createWorkbookWithLimits(InputStream inputStream) throws Exception {
        if (!inputStream.markSupported()) {
            inputStream = new BufferedInputStream(inputStream);
        }
        org.apache.poi.util.IOUtils.setByteArrayMaxOverride((int) MAX_DECOMPRESSED_SIZE);
        return WorkbookFactory.create(inputStream);
    }

    private void validateRowCount(Sheet sheet) {
        if (sheet.getPhysicalNumberOfRows() > MAX_ROWS) {
            throw new LedgerParseException("Sheet exceeds maximum row limit of " + MAX_ROWS);
        }
    }

    private List<LedgerEntry> parsePositionBased(Sheet sheet, String defaultSupplier) {
        List<LedgerEntry> entries = new ArrayList<>();
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            LocalDate date = parseExcelDate(getCellValue(row, 0));
            if (date == null) continue;

            double debit = parseDouble(getCellValue(row, 1));
            double credit = parseDouble(getCellValue(row, 2));
            if (debit <= 0 && credit <= 0) continue;

            String supplier = getCellStringValue(row.getCell(3));
            if (supplier == null || supplier.isBlank()) supplier = defaultSupplier;

            entries.add(LedgerEntry.builder()
                    .date(date)
                    .entryType(debit > 0 ? LedgerEntry.LedgerEntryType.PAYMENT : LedgerEntry.LedgerEntryType.PURCHASE)
                    .supplier(supplier.trim())
                    .amount(debit > 0 ? debit : credit)
                    .build());
        }
        validateNotEmpty(entries);
        return entries;
    }

    private List<LedgerEntry> parseHeaderBased(Sheet sheet, List<String> headers,
                                               int dateIndex, int debitIndex, int creditIndex,
                                               int supplierIndex, String defaultSupplier) {
        List<LedgerEntry> entries = new ArrayList<>();
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            String dateVal = getCellValue(row, dateIndex);
            LocalDate date = parseExcelDate(dateVal);
            if (date == null) continue;

            double debit = debitIndex >= 0 ? parseDouble(getCellValue(row, debitIndex)) : 0;
            double credit = creditIndex >= 0 ? parseDouble(getCellValue(row, creditIndex)) : 0;
            if (debit <= 0 && credit <= 0) continue;

            String supplier = supplierIndex >= 0 ? getCellStringValue(row.getCell(supplierIndex)) : "";
            if (supplier == null || supplier.isBlank()) supplier = defaultSupplier;

            entries.add(LedgerEntry.builder()
                    .date(date)
                    .entryType(debit > 0 ? LedgerEntry.LedgerEntryType.PAYMENT : LedgerEntry.LedgerEntryType.PURCHASE)
                    .supplier(supplier.trim())
                    .amount(debit > 0 ? debit : credit)
                    .build());
        }
        validateNotEmpty(entries);
        return entries;
    }

    private void validateNotEmpty(List<LedgerEntry> entries) {
        if (entries.isEmpty()) {
            throw new LedgerParseException("No valid entries found in Excel file. Check if Date, Debit, and Credit columns have valid data.");
        }
    }

    private static String normalizeColumnName(String name) {
        if (name == null) return "";
        return name.toLowerCase(Locale.ROOT).trim().replaceAll("[^a-z]", "");
    }

    private static String getFileNameWithoutExtension(String filename) {
        if (filename == null) return "Unknown";
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }

    private static int findIndex(List<String> list, java.util.function.Predicate<String> predicate) {
        for (int i = 0; i < list.size(); i++) {
            if (predicate.test(list.get(i))) return i;
        }
        return -1;
    }

    private static LocalDate parseExcelDate(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDate ld) return ld;
        if (value instanceof Date d) {
            return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        if (value instanceof Number n) {
            try {
                Date d = DateUtil.getJavaDate(n.doubleValue());
                return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            } catch (Exception ignored) {
                return null;
            }
        }
        String s = value.toString().trim();
        if (s.isEmpty()) return null;
        try {
            return LocalDate.parse(s);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static double parseDouble(Object value) {
        if (value == null) return 0;
        if (value instanceof Number n) return n.doubleValue();
        String s = value.toString().replaceAll("[^0-9.\\-]", "");
        if (s.isEmpty()) return 0;
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String getCellValue(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex);
        return getCellStringValue(cell);
    }

    private static String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getLocalDateTimeCellValue().toLocalDate().toString()
                    : String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    try {
                        yield cell.getStringCellValue();
                    } catch (Exception e2) {
                        yield "";
                    }
                }
            }
            default -> "";
        };
    }
}
