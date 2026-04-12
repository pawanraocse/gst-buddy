package com.learning.backendservice.domain.ledger;

import com.learning.backendservice.exception.LedgerParseException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.IntStream;

/**
 * Parses Tally/Busy ledger Excel files into a {@link LedgerEntry} list.
 *
 * <p>Supports three formats:
 * <ol>
 *   <li><b>Clean header (row 0)</b>: Date | Debit | Credit | Supplier columns at row 0.</li>
 *   <li><b>Offset header</b>: Metadata rows before headers — scans first
 *       {@value #HEADER_SCAN_DEPTH} rows.</li>
 *   <li><b>Multi-ledger (Tally Creditor export)</b>: Repeated {@code Ledger:} marker rows,
 *       each followed by a sub-header and data rows for one supplier.</li>
 * </ol>
 *
 * <p>Position-based fallback: 4 columns with no credit header →
 * [Date, Debit, Credit, Supplier].
 */
@Component
public class LedgerExcelParser implements LedgerParser {

    private static final Logger log = LoggerFactory.getLogger(LedgerExcelParser.class);

    // ── Limits ──────────────────────────────────────────────────────────────
    private static final long MAX_DECOMPRESSED_SIZE = 100L * 1024 * 1024; // 100 MB
    private static final int MAX_ROWS = 50_000;
    private static final int MAX_ENTRIES_PER_FILE = 20_000;
    private static final int MAX_SUPPLIERS_PER_FILE = 500;

    // Set POI decompression limit once at class load — thread-safe
    static {
        IOUtils.setByteArrayMaxOverride((int) MAX_DECOMPRESSED_SIZE);
    }

    /**
     * How many rows to scan from the top when looking for the header row.
     */
    static final int HEADER_SCAN_DEPTH = 20;

    /**
     * Tally convention marker that precedes each supplier sub-ledger.
     */
    private static final String LEDGER_MARKER = "ledger:";

    // ── Non-transaction row markers (O(1) lookup) ────────────────────────────
    private static final Set<String> NON_TRANSACTION_EXACT = Set.of(
            "cl. bal", "cl bal", "c/d", "c/f", "total"
    );
    private static final List<String> NON_TRANSACTION_PREFIXES = List.of(
            "closing", "grand total", "closing balance", "carried forward",
            "bal c/d", "balance c/d", "balance c/f", "cl. balance"
    );

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

    // ════════════════════════════════════════════════════════════════════════
    //  VALUE OBJECTS
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Carries resolved column indices for a sub-header row found inside a
     * multi-ledger (Tally Creditor) section.
     */
    private record SubHeaderInfo(
            int rowIndex,
            int dateIdx,
            int debitIdx,
            int creditIdx,
            int particularsIdx,
            int invoiceIdx
    ) {
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PUBLIC API
    // ════════════════════════════════════════════════════════════════════════

    @Override
    public List<LedgerEntry> parse(InputStream inputStream, String filename) {
        String defaultSupplier = fileNameWithoutExtension(filename);

        try (Workbook workbook = openWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) {
                throw new LedgerParseException("Excel file is empty");
            }
            validateRowCount(sheet);

            // ── Step 1: Detect multi-ledger format (Tally Creditor export) ──
            int firstLedgerMarker = findFirstLedgerMarkerRow(sheet);
            if (firstLedgerMarker >= 0) {
                log.info("Detected multi-ledger (Tally Creditor) format in '{}', "
                        + "first Ledger: marker at row {}", filename, firstLedgerMarker);
                return parseMultiLedger(sheet, firstLedgerMarker, defaultSupplier);
            }

            // ── Step 2: Auto-detect header row (scan first N rows) ──
            int headerRowIndex = findHeaderRow(sheet);
            if (headerRowIndex < 0) {
                throw new LedgerParseException(
                        "Could not find a header row with a Date column in the first "
                                + HEADER_SCAN_DEPTH + " rows. First row: "
                                + describeRow(sheet.getRow(0)));
            }

            Row headerRow = sheet.getRow(headerRowIndex);
            int colCount = headerRow.getLastCellNum();
            var headers = buildHeaderList(headerRow, colCount);
            var normalized = headers.stream().map(LedgerExcelParser::normalizeColumnName).toList();

            int dateIdx = findIndex(normalized, h -> h.contains("date"));
            int debitIdx = findIndex(normalized, h -> h.contains("debit") || h.equals("dr"));
            int creditIdx = findIndex(normalized, h -> h.contains("credit") || h.equals("cr"));
            int supplierIdx = findIndex(normalized, h ->
                    h.contains("supplier") || h.contains("party")
                            || h.contains("ledger") || h.contains("name"));
            int invoiceIdx = findIndex(normalized, h ->
                    h.contains("vch") || h.contains("ref")
                            || h.contains("invoice") || h.contains("bill"));

            // Position-based fallback: exactly 4 columns with no credit header
            if (colCount == 4 && creditIdx == -1) {
                return parsePositionBased(sheet, headerRowIndex, defaultSupplier);
            }

            if (dateIdx == -1) {
                throw new LedgerParseException(
                        "Could not find Date column. Found headers: " + String.join(", ", headers));
            }
            if (debitIdx == -1 && creditIdx == -1) {
                throw new LedgerParseException(
                        "Could not find Debit or Credit columns. Found headers: "
                                + String.join(", ", headers));
            }

            log.info("Parsed header at row {} in '{}': "
                            + "dateCol={}, debitCol={}, creditCol={}, supplierCol={}, invoiceCol={}",
                    headerRowIndex, filename, dateIdx, debitIdx, creditIdx, supplierIdx, invoiceIdx);

            return parseHeaderBased(sheet, headerRowIndex,
                    dateIdx, debitIdx, creditIdx, supplierIdx, invoiceIdx, defaultSupplier);

        } catch (LedgerParseException e) {
            throw e;
        } catch (Exception e) {
            throw new LedgerParseException("Failed to parse Excel file: " + e.getMessage(), e);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  HEADER ROW AUTO-DETECTION
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Scans the first {@value #HEADER_SCAN_DEPTH} rows for a cell whose normalized
     * value equals {@code "date"}.
     *
     * @return 0-based row index, or -1 if not found
     */
    private int findHeaderRow(Sheet sheet) {
        int scanLimit = Math.min(HEADER_SCAN_DEPTH, sheet.getLastRowNum() + 1);
        for (int i = 0; i < scanLimit; i++) {
            Row row = sheet.getRow(i);
            if (row == null) {
                continue;
            }
            for (int c = 0; c < row.getLastCellNum(); c++) {
                Cell cell = row.getCell(c);
                if (cell != null && "date".equals(normalizeColumnName(getCellStringValue(cell)))) {
                    return i;
                }
            }
        }
        return -1;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  MULTI-LEDGER (TALLY CREDITOR) FORMAT
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Finds the first row whose column-A value starts with {@code "ledger:"} (case-insensitive).
     *
     * @return 0-based row index, or -1 if none found
     */
    private int findFirstLedgerMarkerRow(Sheet sheet) {
        for (int i = 0; i <= sheet.getLastRowNum(); i++) {
            if (isLedgerMarkerRow(sheet.getRow(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Collects the 0-based indices of every row that carries a {@code "Ledger:"} marker,
     * starting from {@code fromRow}.
     */
    private List<Integer> collectMarkerRows(Sheet sheet, int fromRow) {
        var markers = new ArrayList<Integer>();
        for (int i = fromRow; i <= sheet.getLastRowNum(); i++) {
            if (isLedgerMarkerRow(sheet.getRow(i))) {
                markers.add(i);
            }
        }
        return markers;
    }

    private boolean isLedgerMarkerRow(Row row) {
        if (row == null) {
            return false;
        }
        String cellA = getCellStringValue(row.getCell(0)).trim().toLowerCase(Locale.ROOT);
        return cellA.startsWith(LEDGER_MARKER);
    }

    /**
     * Parses a multi-ledger (Tally Creditor) Excel sheet.
     *
     * <p>Structure per supplier section:
     * <pre>
     *   Row N  : Ledger: | SUPPLIER_NAME | date-range | ...
     *   Row N+1: (optional supplier address — skipped)
     *   Row N+2: Date | Particulars | … | Vch Type | Vch No. | Debit | Credit | Balance
     *   Row N+3…: data rows until next "Ledger:" marker
     * </pre>
     */
    private List<LedgerEntry> parseMultiLedger(Sheet sheet, int firstMarkerRow, String defaultSupplier) {
        var markerRows = collectMarkerRows(sheet, firstMarkerRow);

        log.info("Found {} supplier sub-ledgers in file", markerRows.size());

        if (markerRows.size() > MAX_SUPPLIERS_PER_FILE) {
            throw new LedgerParseException(
                    "File contains " + markerRows.size() + " supplier ledgers, max "
                            + MAX_SUPPLIERS_PER_FILE + ". Please split into smaller files.");
        }

        var allEntries = new ArrayList<LedgerEntry>();

        for (int m = 0; m < markerRows.size(); m++) {
            int markerRow = markerRows.get(m);
            int sectionEnd = (m + 1 < markerRows.size())
                    ? markerRows.get(m + 1)
                    : sheet.getLastRowNum() + 1;

            String supplier = extractSupplierName(sheet.getRow(markerRow), defaultSupplier);

            findSubHeader(sheet, markerRow + 1, sectionEnd).ifPresentOrElse(
                    info -> allEntries.addAll(
                            parseSupplierSection(sheet, info, sectionEnd, supplier)),
                    () -> log.warn("No sub-header found for supplier '{}' at marker row {}; skipping",
                            supplier, markerRow)
            );
        }

        validateEntries(allEntries);
        log.info("Multi-ledger parse complete: {} entries across {} suppliers",
                allEntries.size(), markerRows.size());
        return allEntries;
    }

    /**
     * Searches rows [{@code fromRow}, {@code toRow}) for a sub-header containing a
     * {@code "date"} cell and resolves all relevant column indices.
     */
    private Optional<SubHeaderInfo> findSubHeader(Sheet sheet, int fromRow, int toRow) {
        for (int r = fromRow; r < toRow; r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }

            int dateIdx = -1;
            for (int c = 0; c < row.getLastCellNum(); c++) {
                if ("date".equals(normalizeColumnName(getCellStringValue(row.getCell(c))))) {
                    dateIdx = c;
                    break;
                }
            }
            if (dateIdx < 0) {
                continue;
            }

            var normalized = buildNormalizedHeaderList(row);
            int debitIdx = findIndex(normalized, h -> h.contains("debit") || h.equals("dr"));
            int creditIdx = findIndex(normalized, h -> h.contains("credit") || h.equals("cr"));
            int parsIdx = findIndex(normalized, h ->
                    h.contains("particulars") || h.contains("particular"));
            int invoiceIdx = findIndex(normalized, h ->
                    h.contains("vch") || h.contains("ref")
                            || h.contains("invoice") || h.contains("bill"));

            return Optional.of(new SubHeaderInfo(r, dateIdx, debitIdx, creditIdx, parsIdx, invoiceIdx));
        }
        return Optional.empty();
    }

    /**
     * Parses data rows for a single supplier section using the resolved
     * {@link SubHeaderInfo}.
     */
    private List<LedgerEntry> parseSupplierSection(Sheet sheet, SubHeaderInfo info,
                                                   int sectionEnd, String supplier) {
        if (info.debitIdx() == -1 && info.creditIdx() == -1) {
            log.warn("No Debit/Credit columns for supplier '{}'; skipping section", supplier);
            return List.of();
        }

        var entries = new ArrayList<LedgerEntry>();

        for (int r = info.rowIndex() + 1; r < sectionEnd; r++) {
            Row dataRow = sheet.getRow(r);
            if (dataRow == null || isNonTransactionRow(dataRow)) {
                continue;
            }

            LocalDate date = parseExcelDate(getRawCellValue(dataRow, info.dateIdx()));
            if (date == null) {
                continue;
            }

            BigDecimal debit = info.debitIdx() >= 0 ? parseBigDecimal(getRawCellValue(dataRow, info.debitIdx())) : BigDecimal.ZERO;
            BigDecimal credit = info.creditIdx() >= 0 ? parseBigDecimal(getRawCellValue(dataRow, info.creditIdx())) : BigDecimal.ZERO;
            if (debit.signum() <= 0 && credit.signum() <= 0) {
                continue;
            }

            var entryType = determineEntryType(dataRow, info.particularsIdx(), debit, credit);
            var amount = debit.signum() > 0 ? debit : credit;
            var invoiceNumber = info.invoiceIdx() >= 0
                    ? getCellStringValue(dataRow.getCell(info.invoiceIdx())).trim() : "";

            entries.add(LedgerEntry.builder()
                    .date(date)
                    .entryType(entryType)
                    .supplier(supplier)
                    .amount(amount)
                    .invoiceNumber(invoiceNumber.isEmpty() ? null : invoiceNumber)
                    .build());
        }

        return entries;
    }

    /**
     * Resolves entry type using Tally's To/By convention from the Particulars column.
     *
     * <ul>
     *   <li>{@code "To"} → Payment made (debit entry in creditor ledger)</li>
     *   <li>{@code "By"} → Purchase/Invoice received (credit entry in creditor ledger)</li>
     * </ul>
     * Falls back to Debit/Credit amount magnitude when Particulars is absent.
     */
    private LedgerEntry.LedgerEntryType determineEntryType(Row row, int particularsIdx,
                                                           BigDecimal debit, BigDecimal credit) {
        if (particularsIdx >= 0) {
            String particulars = getCellStringValue(row.getCell(particularsIdx))
                    .trim().toLowerCase(Locale.ROOT);
            if ("to".equals(particulars)) {
                return LedgerEntry.LedgerEntryType.PAYMENT;
            }
            if ("by".equals(particulars)) {
                return LedgerEntry.LedgerEntryType.PURCHASE;
            }
        }
        return debit.signum() > 0 ? LedgerEntry.LedgerEntryType.PAYMENT
                : LedgerEntry.LedgerEntryType.PURCHASE;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  STANDARD PARSING
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Position-based parsing for simple 4-column files without an explicit
     * credit header. Columns assumed: [Date, Debit, Credit, Supplier].
     */
    private List<LedgerEntry> parsePositionBased(Sheet sheet, int headerRowIndex,
                                                 String defaultSupplier) {
        log.warn("Position-based fallback: sheet '{}' has no invoice column — "
                + "entries will lack invoice numbers", sheet.getSheetName());

        var entries = new ArrayList<LedgerEntry>();
        for (int i = headerRowIndex + 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isNonTransactionRow(row)) {
                continue;
            }

            LocalDate date = parseExcelDate(getRawCellValue(row, 0));
            if (date == null) {
                continue;
            }

            BigDecimal debit = parseBigDecimal(getRawCellValue(row, 1));
            BigDecimal credit = parseBigDecimal(getRawCellValue(row, 2));
            if (debit.signum() <= 0 && credit.signum() <= 0) {
                continue;
            }

            String supplier = getCellStringValue(row.getCell(3));
            if (supplier == null || supplier.isBlank()) {
                supplier = defaultSupplier;
            }

            entries.add(LedgerEntry.builder()
                    .date(date)
                    .entryType(debit.signum() > 0 ? LedgerEntry.LedgerEntryType.PAYMENT
                            : LedgerEntry.LedgerEntryType.PURCHASE)
                    .supplier(supplier.trim())
                    .amount(debit.signum() > 0 ? debit : credit)
                    .build());
        }

        validateEntries(entries);
        return entries;
    }

    /**
     * Header-based parsing using detected column indices.
     */
    private List<LedgerEntry> parseHeaderBased(Sheet sheet, int headerRowIndex,
                                               int dateIdx, int debitIdx, int creditIdx,
                                               int supplierIdx, int invoiceIdx,
                                               String defaultSupplier) {
        var entries = new ArrayList<LedgerEntry>();

        for (int i = headerRowIndex + 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isNonTransactionRow(row)) {
                continue;
            }

            LocalDate date = parseExcelDate(getRawCellValue(row, dateIdx));
            if (date == null) {
                continue;
            }

            BigDecimal debit = debitIdx >= 0 ? parseBigDecimal(getRawCellValue(row, debitIdx)) : BigDecimal.ZERO;
            BigDecimal credit = creditIdx >= 0 ? parseBigDecimal(getRawCellValue(row, creditIdx)) : BigDecimal.ZERO;
            if (debit.signum() <= 0 && credit.signum() <= 0) {
                continue;
            }

            String supplier = supplierIdx >= 0 ? getCellStringValue(row.getCell(supplierIdx)) : "";
            if (supplier == null || supplier.isBlank()) {
                supplier = defaultSupplier;
            }

            String invoiceNumber = invoiceIdx >= 0
                    ? getCellStringValue(row.getCell(invoiceIdx)).trim() : "";

            entries.add(LedgerEntry.builder()
                    .date(date)
                    .entryType(debit.signum() > 0 ? LedgerEntry.LedgerEntryType.PAYMENT
                            : LedgerEntry.LedgerEntryType.PURCHASE)
                    .supplier(supplier.trim())
                    .amount(debit.signum() > 0 ? debit : credit)
                    .invoiceNumber(invoiceNumber.isEmpty() ? null : invoiceNumber)
                    .build());
        }

        validateEntries(entries);
        return entries;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  DATE PARSING
    //  Handles Excel serials, ISO-8601, and common Indian text formats.
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Parses a raw cell value (Object, not pre-stringified) into a {@link LocalDate}.
     *
     * <p>Keeps type fidelity: a NUMERIC date cell returns a {@link LocalDate} directly
     * from POI without round-tripping through a string serial representation.
     *
     * <ul>
     *   <li>{@link LocalDate} → returned as-is</li>
     *   <li>{@link Date}      → converted via system timezone</li>
     *   <li>{@link Number}    → treated as Excel serial date double</li>
     *   <li>{@link String}    → ISO-8601 first, then fallback formatters</li>
     * </ul>
     *
     * @return parsed date, or {@code null} if value is null / unparseable
     */
    public static LocalDate parseExcelDate(Object value) {
        if (value == null) {
            return null;
        }
        switch (value) {
            case LocalDate ld -> {
                return ld;
            }
            case Date d -> {
                return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            }
            case Number n -> {
                return parseExcelSerialDate(n.doubleValue());
            }
            default -> {
            }
        }

        String s = value.toString().trim();
        if (s.isEmpty()) {
            return null;
        }

        // Numeric string that looks like an Excel serial, e.g. "44652.0"
        try {
            double serial = Double.parseDouble(s);
            if (serial > 0 && serial < 100_000) {
                LocalDate parsed = parseExcelSerialDate(serial);
                if (parsed != null) {
                    return parsed;
                }
            }
        } catch (NumberFormatException ignored) {
            // Not a number — fall through to text formats
        }

        // ISO-8601 (yyyy-MM-dd)
        try {
            return LocalDate.parse(s);
        } catch (DateTimeParseException ignored) {
            // fall through
        }

        // Indian / common text date formats
        for (DateTimeFormatter formatter : FALLBACK_DATE_FORMATTERS) {
            try {
                return LocalDate.parse(s, formatter);
            } catch (DateTimeParseException ignored) {
                // try next
            }
        }

        return null;
    }

    /**
     * Converts an Excel serial date number to a {@link LocalDate}. Returns {@code null} for invalid serials.
     */
    private static LocalDate parseExcelSerialDate(double serial) {
        if (serial <= 0) {
            return null;
        }
        try {
            Date d = DateUtil.getJavaDate(serial);
            return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        } catch (Exception ignored) {
            return null;
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  CELL VALUE EXTRACTION
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Extracts a raw typed value from a cell, preserving type fidelity for date parsing.
     *
     * <ul>
     *   <li>NUMERIC date-formatted cell → {@link LocalDate}</li>
     *   <li>NUMERIC non-date cell       → {@link Double}</li>
     *   <li>STRING cell                 → {@link String}</li>
     *   <li>FORMULA cell               → evaluated numeric or string</li>
     *   <li>null / blank               → {@code null}</li>
     * </ul>
     */
    private static Object getRawCellValue(Row row, int colIndex) {
        if (row == null || colIndex < 0) {
            return null;
        }
        Cell cell = row.getCell(colIndex);
        if (cell == null) {
            return null;
        }

        return switch (cell.getCellType()) {
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getLocalDateTimeCellValue().toLocalDate()
                    : cell.getNumericCellValue();
            case STRING -> cell.getStringCellValue();
            case BOOLEAN -> cell.getBooleanCellValue();
            case FORMULA -> evaluateFormulaCellSafely(cell);
            default -> null;
        };
    }

    /**
     * Safe formula-cell evaluation: tries numeric first, then string, returns {@code null} on failure.
     */
    private static Object evaluateFormulaCellSafely(Cell cell) {
        try {
            return cell.getNumericCellValue();
        } catch (Exception ignored) {
            // ignored
        }
        try {
            return cell.getStringCellValue();
        } catch (Exception ignored) {
            // ignored
        }
        return null;
    }

    /**
     * Returns the string representation of a cell value.
     * Delegates to {@link #getRawCellValue} to keep extraction logic in one place.
     */
    static String getCellStringValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        Object raw = getRawCellValue(cell.getRow(), cell.getColumnIndex());
        return raw == null ? "" : raw.toString();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  NON-TRANSACTION ROW DETECTION
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Returns {@code true} if the row is a summary/total row that should be skipped.
     * Scans ALL cells because Tally/Busy may place markers in any column.
     */
    private static boolean isNonTransactionRow(Row row) {
        if (row == null) {
            return true;
        }
        for (int c = 0; c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell == null) {
                continue;
            }
            String val = getCellStringValue(cell).trim().toLowerCase(Locale.ROOT);
            if (val.isEmpty()) {
                continue;
            }
            if (NON_TRANSACTION_EXACT.contains(val)) {
                return true;
            }
            if (NON_TRANSACTION_PREFIXES.stream().anyMatch(val::startsWith)) {
                return true;
            }
        }
        return false;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  VALIDATION
    // ════════════════════════════════════════════════════════════════════════

    private static void validateRowCount(Sheet sheet) {
        if (sheet.getPhysicalNumberOfRows() > MAX_ROWS) {
            throw new LedgerParseException("Sheet exceeds maximum row limit of " + MAX_ROWS);
        }
    }

    /**
     * Validates that the entry list is non-empty and within the per-file size cap.
     * These are two distinct concerns intentionally kept in a single guard method
     * to avoid partial-parse states being returned to callers.
     */
    private static void validateEntries(List<LedgerEntry> entries) {
        if (entries.isEmpty()) {
            throw new LedgerParseException(
                    "No valid entries found in Excel file. "
                            + "Check if Date, Debit, and Credit columns have valid data.");
        }
        if (entries.size() > MAX_ENTRIES_PER_FILE) {
            throw new LedgerParseException(
                    "File contains " + entries.size() + " transactions, max "
                            + MAX_ENTRIES_PER_FILE + ". Please split into smaller files.");
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  NUMERIC PARSING
    // ════════════════════════════════════════════════════════════════════════

    private static BigDecimal parseBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }

        return switch (value) {
            case BigDecimal bd -> bd;
            case Number n -> toBigDecimalSafely(n);
            default -> parseFromString(value.toString()); // value is guaranteed non-null here
        };
    }

    /**
     * Parses a string cell value to {@link BigDecimal}, stripping non-numeric
     * characters (currency symbols, thousand separators, etc.).
     *
     * <p>The regex {@code [^0-9.-]} strips everything except digits, decimal point,
     * and a leading minus. Hyphen is placed at end of the character class to
     * avoid misinterpretation as a range operator.
     */
    private static BigDecimal parseFromString(String raw) {
        String sanitized = raw.replaceAll("[^0-9.-]", "").trim();
        if (sanitized.isEmpty() || sanitized.equals("-") || sanitized.equals(".")) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(sanitized);
        } catch (NumberFormatException ignored) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * Converts a {@link Number} to {@link BigDecimal} without losing precision.
     *
     * <p>Avoids {@code BigDecimal.valueOf(n.doubleValue())} which silently truncates
     * precision for {@code Float} and large {@code Long} values. Uses the string
     * representation which preserves the original significant digits.
     *
     * <p>Falls back to {@code doubleValue()} only if {@code toString()} produces
     * an unparseable representation (e.g. "Infinity", "NaN").
     */
    private static BigDecimal toBigDecimalSafely(Number n) {
        try {
            return new BigDecimal(n.toString());
        } catch (NumberFormatException ignored) {
            // NaN / Infinity — treat as zero
            return BigDecimal.ZERO;
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════════════════════════

    private static Workbook openWorkbook(InputStream inputStream) throws Exception {
        if (!inputStream.markSupported()) {
            inputStream = new BufferedInputStream(inputStream);
        }
        return WorkbookFactory.create(inputStream);
    }

    /**
     * Extracts the supplier name from cell B of a marker row.
     * Strips trailing CR/LF characters introduced by some Tally exports.
     */
    private static String extractSupplierName(Row markerRow, String fallback) {
        if (markerRow == null) {
            return fallback;
        }
        String name = getCellStringValue(markerRow.getCell(1)).trim();
        name = name.replaceAll("[\r\n]+", " ").trim();
        return name.isEmpty() ? fallback : name;
    }

    /**
     * Builds a list of raw header strings from a row's cells.
     */
    private static List<String> buildHeaderList(Row row, int colCount) {
        var headers = new ArrayList<String>(colCount);
        for (int i = 0; i < colCount; i++) {
            Cell cell = row.getCell(i);
            headers.add(cell != null ? getCellStringValue(cell) : "");
        }
        return headers;
    }

    /**
     * Builds a normalized (lowercase, letters-only) header list from all cells in a row.
     */
    private static List<String> buildNormalizedHeaderList(Row row) {
        var normalized = new ArrayList<String>(row.getLastCellNum());
        for (int c = 0; c < row.getLastCellNum(); c++) {
            normalized.add(normalizeColumnName(getCellStringValue(row.getCell(c))));
        }
        return normalized;
    }

    /**
     * Normalizes a column name to lowercase ASCII letters only (strips digits, spaces, symbols).
     */
    static String normalizeColumnName(String name) {
        if (name == null) {
            return "";
        }
        return name.toLowerCase(Locale.ROOT).trim().replaceAll("[^a-z]", "");
    }

    /**
     * Strips file extension and path separators from a filename to produce a supplier fallback.
     * Uses {@link Path} to handle both Unix and Windows path separators correctly.
     */
    private static String fileNameWithoutExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            return "Unknown";
        }
        String name = Path.of(filename).getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    /**
     * Finds the first index in {@code list} matching {@code predicate}.
     * Uses {@link IntStream} for idiomatic Java 21.
     *
     * @return 0-based index, or -1 if no match
     */
    private static int findIndex(List<String> list, Predicate<String> predicate) {
        return IntStream.range(0, list.size())
                .filter(i -> predicate.test(list.get(i)))
                .findFirst()
                .orElse(-1);
    }

    /**
     * Produces a human-readable summary of row cell values (used in error messages).
     */
    private static String describeRow(Row row) {
        if (row == null) {
            return "(empty row)";
        }
        var vals = new ArrayList<String>(row.getLastCellNum());
        for (int c = 0; c < row.getLastCellNum(); c++) {
            vals.add(getCellStringValue(row.getCell(c)));
        }
        return String.join(", ", vals);
    }
}
