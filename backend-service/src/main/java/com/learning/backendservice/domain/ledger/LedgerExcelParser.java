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
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Parses Tally/Busy ledger Excel files into LedgerEntry list.
 *
 * <p>Supports three formats:
 * <ol>
 *   <li><b>Clean header (row 0)</b>: Date | Debit | Credit | Supplier columns at row 0.</li>
 *   <li><b>Offset header</b>: Metadata rows before headers — scans first {@value #HEADER_SCAN_DEPTH} rows.</li>
 *   <li><b>Multi-ledger (Tally Creditor export)</b>: Repeated {@code Ledger:} marker rows,
 *       each followed by a sub-header and data rows for one supplier.</li>
 * </ol>
 *
 * <p>Position-based fallback: 4 columns with no credit header → [Date, Debit, Credit, Supplier].
 */
@Component
public class LedgerExcelParser implements LedgerParser {

    private static final Logger log = LoggerFactory.getLogger(LedgerExcelParser.class);

    private static final long MAX_DECOMPRESSED_SIZE = 100L * 1024 * 1024; // 100 MB
    private static final int MAX_ROWS = 50_000;

    /** How many rows to scan from the top when looking for the header row. */
    static final int HEADER_SCAN_DEPTH = 20;

    /** Tally convention marker that precedes each supplier sub-ledger. */
    private static final String LEDGER_MARKER = "ledger:";

    /**
     * Fallback date formatters for Indian text dates.
     * Tried in order after ISO-8601 and Excel-serial parsing fail.
     */
    private static final DateTimeFormatter[] FALLBACK_DATE_FORMATTERS = {
            DateTimeFormatter.ofPattern("d-MMM-yy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d-MMM-yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("d-M-yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
    };

    @Override
    public List<LedgerEntry> parse(InputStream inputStream, String filename) {
        String defaultSupplier = getFileNameWithoutExtension(filename);

        try (Workbook workbook = createWorkbookWithLimits(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) {
                throw new LedgerParseException("Excel file is empty");
            }
            validateRowCount(sheet);

            // ── Step 1: Detect multi-ledger format (Tally Creditor export) ──
            int firstLedgerMarker = findFirstLedgerMarkerRow(sheet);
            if (firstLedgerMarker >= 0) {
                log.info("Detected multi-ledger (Tally Creditor) format in '{}', first Ledger: marker at row {}",
                        filename, firstLedgerMarker);
                return parseMultiLedger(sheet, firstLedgerMarker, defaultSupplier);
            }

            // ── Step 2: Auto-detect header row (scan first N rows) ──
            int headerRowIndex = findHeaderRow(sheet);
            if (headerRowIndex < 0) {
                String firstRowContent = describeRow(sheet.getRow(0));
                throw new LedgerParseException(
                        "Could not find a header row with a Date column in the first "
                                + HEADER_SCAN_DEPTH + " rows. First row: " + firstRowContent);
            }

            Row headerRow = sheet.getRow(headerRowIndex);
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
            int debitIndex = findIndex(normalizedHeaders, h -> h.contains("debit") || h.equals("dr"));
            int creditIndex = findIndex(normalizedHeaders, h -> h.contains("credit") || h.equals("cr"));
            int supplierIndex = findIndex(normalizedHeaders, h ->
                    h.contains("supplier") || h.contains("party") || h.contains("ledger") || h.contains("name"));

            // Position-based fallback: exactly 4 columns, no credit header
            if (colCount == 4 && creditIndex == -1) {
                return parsePositionBased(sheet, headerRowIndex, defaultSupplier);
            }

            if (dateIndex == -1) {
                throw new LedgerParseException("Could not find Date column. Found headers: " + String.join(", ", headers));
            }
            if (debitIndex == -1 && creditIndex == -1) {
                throw new LedgerParseException("Could not find Debit or Credit columns. Found headers: " + String.join(", ", headers));
            }

            log.info("Parsed header at row {} in '{}': dateCol={}, debitCol={}, creditCol={}, supplierCol={}",
                    headerRowIndex, filename, dateIndex, debitIndex, creditIndex, supplierIndex);

            return parseHeaderBased(sheet, headerRowIndex, dateIndex, debitIndex, creditIndex, supplierIndex, defaultSupplier);

        } catch (LedgerParseException e) {
            throw e;
        } catch (Exception e) {
            throw new LedgerParseException("Failed to parse Excel file: " + e.getMessage(), e);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  HEADER ROW AUTO-DETECTION
    // ═══════════════════════════════════════════════════════════════

    /**
     * Scans the first {@value #HEADER_SCAN_DEPTH} rows for one whose normalized cells
     * include a "date" column. Returns the 0-based row index, or -1 if not found.
     */
    private int findHeaderRow(Sheet sheet) {
        int scanLimit = Math.min(HEADER_SCAN_DEPTH, sheet.getLastRowNum() + 1);
        for (int i = 0; i < scanLimit; i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            for (int c = 0; c < row.getLastCellNum(); c++) {
                Cell cell = row.getCell(c);
                if (cell != null) {
                    String normalized = normalizeColumnName(getCellStringValue(cell));
                    if (normalized.equals("date")) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    // ═══════════════════════════════════════════════════════════════
    //  MULTI-LEDGER (TALLY CREDITOR) FORMAT
    // ═══════════════════════════════════════════════════════════════

    /**
     * Finds the first row whose column A contains "Ledger:" (case-insensitive).
     * Searches the entire sheet so we don't miss deeply nested markers.
     *
     * @return 0-based row index, or -1 if none found
     */
    private int findFirstLedgerMarkerRow(Sheet sheet) {
        // Search the full sheet — Creditor exports may have the marker well past row 20
        for (int i = 0; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            String cellA = getCellStringValue(row.getCell(0)).trim().toLowerCase(Locale.ROOT);
            if (cellA.equals(LEDGER_MARKER) || cellA.startsWith("ledger:")) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Parses a multi-ledger (Tally Creditor) Excel sheet.
     *
     * <p>Structure per supplier section:
     * <pre>
     *   Row N  : Ledger: | SUPPLIER_NAME | date-range | ...
     *   Row N+1: (optional supplier address — skipped)
     *   Row N+2: Date | Particulars | ... | Vch Type | Vch No. | Debit | Credit | Balance  ← sub-header
     *   Row N+3…: data rows until next "Ledger:" marker
     * </pre>
     *
     * <p>Tally convention for determining debit vs credit:
     * <ul>
     *   <li>{@code "To"} in Particulars column (col B) → <b>Payment</b> (debit by the reporting entity)</li>
     *   <li>{@code "By"} in Particulars column (col B) → <b>Purchase</b> (credit / invoice received)</li>
     *   <li>Fallback: if Debit column has a value → Payment; else if Credit → Purchase</li>
     * </ul>
     */
    private List<LedgerEntry> parseMultiLedger(Sheet sheet, int firstMarkerRow, String defaultSupplier) {
        List<LedgerEntry> allEntries = new ArrayList<>();

        // Collect all Ledger: marker row indices
        List<Integer> markerRows = new ArrayList<>();
        for (int i = firstMarkerRow; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            String cellA = getCellStringValue(row.getCell(0)).trim().toLowerCase(Locale.ROOT);
            if (cellA.equals(LEDGER_MARKER) || cellA.startsWith("ledger:")) {
                markerRows.add(i);
            }
        }

        log.info("Found {} supplier sub-ledgers in file", markerRows.size());

        for (int m = 0; m < markerRows.size(); m++) {
            int markerRow = markerRows.get(m);
            int sectionEnd = (m + 1 < markerRows.size()) ? markerRows.get(m + 1) : sheet.getLastRowNum() + 1;

            // Extract supplier name from cell B of the marker row
            Row marker = sheet.getRow(markerRow);
            String supplier = marker != null ? getCellStringValue(marker.getCell(1)).trim() : "";
            if (supplier.isEmpty()) {
                supplier = defaultSupplier;
            }
            // Clean up supplier name (some Tally exports have trailing \r\n or whitespace)
            supplier = supplier.replaceAll("[\\r\\n]+", " ").trim();

            // Find the sub-header row (contains "Date") within this section
            int subHeaderRow = -1;
            int debitIdx = -1;
            int creditIdx = -1;
            int dateIdx = -1;
            int particularsIdx = -1;

            for (int r = markerRow + 1; r < sectionEnd; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                // Check if this row is a sub-header
                for (int c = 0; c < row.getLastCellNum(); c++) {
                    String val = normalizeColumnName(getCellStringValue(row.getCell(c)));
                    if (val.equals("date")) {
                        subHeaderRow = r;
                        dateIdx = c;
                        break;
                    }
                }
                if (subHeaderRow >= 0) break;
            }

            if (subHeaderRow < 0) {
                log.warn("No sub-header found for supplier '{}' at marker row {}; skipping section", supplier, markerRow);
                continue;
            }

            // Resolve column indices from sub-header
            Row subHeader = sheet.getRow(subHeaderRow);
            List<String> subNormalized = new ArrayList<>();
            for (int c = 0; c < subHeader.getLastCellNum(); c++) {
                subNormalized.add(normalizeColumnName(getCellStringValue(subHeader.getCell(c))));
            }

            debitIdx = findIndex(subNormalized, h -> h.contains("debit") || h.equals("dr"));
            creditIdx = findIndex(subNormalized, h -> h.contains("credit") || h.equals("cr"));
            particularsIdx = findIndex(subNormalized, h -> h.contains("particulars") || h.contains("particular"));

            if (debitIdx == -1 && creditIdx == -1) {
                log.warn("No Debit/Credit columns for supplier '{}'; skipping section", supplier);
                continue;
            }

            // Parse data rows for this supplier section
            for (int r = subHeaderRow + 1; r < sectionEnd; r++) {
                Row dataRow = sheet.getRow(r);
                if (dataRow == null) continue;

                // Skip sub-summary/total rows (often at end of section)
                String cellAValue = getCellStringValue(dataRow.getCell(0)).trim().toLowerCase(Locale.ROOT);
                if (cellAValue.startsWith("total") || cellAValue.startsWith("closing")
                        || cellAValue.startsWith("opening")) {
                    continue;
                }

                LocalDate date = parseExcelDate(getCellValue(dataRow, dateIdx));
                if (date == null) continue;

                double debit = debitIdx >= 0 ? parseDouble(getCellValue(dataRow, debitIdx)) : 0;
                double credit = creditIdx >= 0 ? parseDouble(getCellValue(dataRow, creditIdx)) : 0;
                if (debit <= 0 && credit <= 0) continue;

                // Determine entry type using Tally's To/By convention
                LedgerEntry.LedgerEntryType entryType = determineEntryType(dataRow, particularsIdx, debit, credit);
                double amount = debit > 0 ? debit : credit;

                allEntries.add(LedgerEntry.builder()
                        .date(date)
                        .entryType(entryType)
                        .supplier(supplier)
                        .amount(amount)
                        .build());
            }
        }

        validateNotEmpty(allEntries);
        log.info("Multi-ledger parse complete: {} entries across {} suppliers",
                allEntries.size(), markerRows.size());
        return allEntries;
    }

    /**
     * Determines ledger entry type using Tally's To/By convention from the Particulars column.
     * <p>Tally convention:
     * <ul>
     *   <li>"To" = Payment made (debit entry in creditor ledger)</li>
     *   <li>"By" = Purchase/Invoice received (credit entry in creditor ledger)</li>
     * </ul>
     * Falls back to Debit/Credit amount if Particulars is unavailable.
     */
    private LedgerEntry.LedgerEntryType determineEntryType(Row row, int particularsIdx, double debit, double credit) {
        if (particularsIdx >= 0) {
            String particulars = getCellStringValue(row.getCell(particularsIdx)).trim().toLowerCase(Locale.ROOT);
            if (particulars.equals("to")) {
                return LedgerEntry.LedgerEntryType.PAYMENT;
            }
            if (particulars.equals("by")) {
                return LedgerEntry.LedgerEntryType.PURCHASE;
            }
        }
        // Fallback: debit > 0 means payment out, credit > 0 means purchase in
        return debit > 0 ? LedgerEntry.LedgerEntryType.PAYMENT : LedgerEntry.LedgerEntryType.PURCHASE;
    }

    // ═══════════════════════════════════════════════════════════════
    //  STANDARD PARSING (OFFSET HEADER SUPPORT)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Position-based parsing for simple 4-column files without explicit credit header.
     * Columns: [Date, Debit, Credit, Supplier].
     *
     * @param headerRowIndex the detected header row index (data starts at headerRowIndex + 1)
     */
    private List<LedgerEntry> parsePositionBased(Sheet sheet, int headerRowIndex, String defaultSupplier) {
        List<LedgerEntry> entries = new ArrayList<>();
        for (int i = headerRowIndex + 1; i <= sheet.getLastRowNum(); i++) {
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

    /**
     * Header-based parsing using detected column indices.
     *
     * @param headerRowIndex the detected header row index (data starts at headerRowIndex + 1)
     */
    private List<LedgerEntry> parseHeaderBased(Sheet sheet, int headerRowIndex,
                                               int dateIndex, int debitIndex, int creditIndex,
                                               int supplierIndex, String defaultSupplier) {
        List<LedgerEntry> entries = new ArrayList<>();
        for (int i = headerRowIndex + 1; i <= sheet.getLastRowNum(); i++) {
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

    // ═══════════════════════════════════════════════════════════════
    //  UTILITY METHODS
    // ═══════════════════════════════════════════════════════════════

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

    private void validateNotEmpty(List<LedgerEntry> entries) {
        if (entries.isEmpty()) {
            throw new LedgerParseException("No valid entries found in Excel file. "
                    + "Check if Date, Debit, and Credit columns have valid data.");
        }
    }

    /** Normalizes a column name to lowercase letters only (strips digits, spaces, symbols). */
    static String normalizeColumnName(String name) {
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

    /** Produces a human-readable summary of a row's cell values (for error messages). */
    private String describeRow(Row row) {
        if (row == null) return "(empty row)";
        List<String> vals = new ArrayList<>();
        for (int c = 0; c < row.getLastCellNum(); c++) {
            vals.add(getCellStringValue(row.getCell(c)));
        }
        return String.join(", ", vals);
    }

    // ═══════════════════════════════════════════════════════════════
    //  DATE PARSING — supports Excel serial numbers, ISO-8601,
    //                  and common Indian text formats.
    // ═══════════════════════════════════════════════════════════════

    /**
     * Parses a cell value into a {@link LocalDate}. Handles:
     * <ul>
     *   <li>Already a {@code LocalDate} or {@code Date} → direct conversion</li>
     *   <li>Numeric (Excel serial date, e.g. 44652) → via {@link DateUtil#getJavaDate}</li>
     *   <li>ISO-8601 string (2022-04-01) → via {@link LocalDate#parse}</li>
     *   <li>Indian text formats (1-Apr-20, 01/04/2020, etc.) → via fallback formatters</li>
     *   <li>Numeric string that looks like an Excel serial ("44652.0") → parsed as serial</li>
     * </ul>
     *
     * @return parsed date, or null if unparseable
     */
    static LocalDate parseExcelDate(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDate ld) return ld;
        if (value instanceof Date d) {
            return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        if (value instanceof Number n) {
            return parseExcelSerialDate(n.doubleValue());
        }

        String s = value.toString().trim();
        if (s.isEmpty()) return null;

        // Try as a numeric string (e.g. "44652" or "44652.0" from getCellStringValue)
        try {
            double serial = Double.parseDouble(s);
            // Excel serial dates are in a reasonable range (1 = 1900-01-01 to ~73050 = 2099-12-31)
            if (serial > 0 && serial < 100_000) {
                LocalDate parsed = parseExcelSerialDate(serial);
                if (parsed != null) return parsed;
            }
        } catch (NumberFormatException ignored) {
            // Not a number — try text formats
        }

        // Try ISO-8601 (yyyy-MM-dd)
        try {
            return LocalDate.parse(s);
        } catch (DateTimeParseException ignored) {
            // fall through
        }

        // Try Indian/common text date formats
        for (DateTimeFormatter formatter : FALLBACK_DATE_FORMATTERS) {
            try {
                return LocalDate.parse(s, formatter);
            } catch (DateTimeParseException ignored) {
                // try next
            }
        }

        return null;
    }

    /** Converts an Excel serial date number to a LocalDate. Returns null for invalid values. */
    private static LocalDate parseExcelSerialDate(double serial) {
        if (serial <= 0) return null;
        try {
            Date d = DateUtil.getJavaDate(serial);
            return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
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

    static String getCellStringValue(Cell cell) {
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
