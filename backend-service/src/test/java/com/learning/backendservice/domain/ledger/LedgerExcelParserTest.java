package com.learning.backendservice.domain.ledger;
import java.math.BigDecimal;
import com.learning.backendservice.exception.LedgerParseException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link LedgerExcelParser}.
 *
 * <p>Uses in-memory Apache POI workbooks to test all three supported formats:
 * <ol>
 *   <li>Clean header at row 0 (standard export)</li>
 *   <li>Offset header (metadata rows before header)</li>
 *   <li>Multi-ledger Tally Creditor export (Ledger: markers)</li>
 * </ol>
 */
@DisplayName("LedgerExcelParser — Robust Format Support")
class LedgerExcelParserTest {

    private LedgerExcelParser parser;

    @BeforeEach
    void setUp() {
        parser = new LedgerExcelParser();
    }

    // ═══════════════════════════════════════════════════════════════
    //  HELPER METHODS — Build in-memory Excel workbooks
    // ═══════════════════════════════════════════════════════════════

    private byte[] toBytes(Workbook workbook) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    private List<LedgerEntry> parse(byte[] excelBytes, String filename) {
        return parser.parse(new ByteArrayInputStream(excelBytes), filename);
    }

    private void setCells(Row row, Object... values) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == null) {
                // leave cell empty
            } else if (values[i] instanceof String s) {
                row.createCell(i).setCellValue(s);
            } else if (values[i] instanceof Number n) {
                row.createCell(i).setCellValue(n.doubleValue());
            } else if (values[i] instanceof LocalDate ld) {
                // Store as Excel serial number (numeric)
                row.createCell(i).setCellValue(org.apache.poi.ss.usermodel.DateUtil.getExcelDate(
                        java.util.Date.from(ld.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())));
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  1. CLEAN HEADER AT ROW 0 (Regression tests)
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Clean Header Format (row 0 = header)")
    class CleanHeaderFormat {

        @Test
        @DisplayName("Standard 4-column (Date, Debit, Credit, Name) with numeric dates → parses correctly")
        void standardFourColumnsWithNumericDates() throws Exception {
            Workbook wb = new XSSFWorkbook();
            Sheet sheet = wb.createSheet();

            setCells(sheet.createRow(0), "Date", "Debit", "Credit", "Name");
            // Excel serial 44652 = 2022-04-01
            setCells(sheet.createRow(1), 44652.0, 1063.0, null, "KD");
            setCells(sheet.createRow(2), 44658.0, 500000.0, null, "KD");
            setCells(sheet.createRow(3), 44690.0, null, 967172.0, "KD");

            List<LedgerEntry> entries = parse(toBytes(wb), "KD STEEL.xlsx");

            assertThat(entries).hasSize(3);
            assertThat(entries.get(0).getDate()).isEqualTo(LocalDate.of(2022, 4, 1));
            assertThat(entries.get(0).getAmount().compareTo(BigDecimal.valueOf(1063.0))).isZero();
            assertThat(entries.get(0).getEntryType()).isEqualTo(LedgerEntry.LedgerEntryType.PAYMENT);
            assertThat(entries.get(0).getSupplier()).isEqualTo("KD");
            assertThat(entries.get(2).getEntryType()).isEqualTo(LedgerEntry.LedgerEntryType.PURCHASE);
        }

        @Test
        @DisplayName("File containing 'Vch No.' successfully maps to invoiceNumber")
        void standardWithInvoiceNumber() throws Exception {
            Workbook wb = new XSSFWorkbook();
            Sheet sheet = wb.createSheet();

            setCells(sheet.createRow(0), "Date", "Vch No.", "Debit", "Credit", "Name");
            setCells(sheet.createRow(1), 44652.0, "INV-001", null, 1000.0, "KD");

            List<LedgerEntry> entries = parse(toBytes(wb), "KD STEEL.xlsx");

            assertThat(entries).hasSize(1);
            assertThat(entries.get(0).getInvoiceNumber()).isEqualTo("INV-001");
            assertThat(entries.get(0).getEntryType()).isEqualTo(LedgerEntry.LedgerEntryType.PURCHASE);
        }

        @Test
        @DisplayName("Missing supplier → falls back to filename without extension")
        void missingSupplierFallsBackToFilename() throws Exception {
            Workbook wb = new XSSFWorkbook();
            Sheet sheet = wb.createSheet();

            setCells(sheet.createRow(0), "Date", "Debit", "Credit", "Name");
            setCells(sheet.createRow(1), 44652.0, 1000.0, null, "");
            setCells(sheet.createRow(2), 44653.0, 2000.0, null, null);

            List<LedgerEntry> entries = parse(toBytes(wb), "MY COMPANY.xlsx");

            assertThat(entries).hasSize(2);
            assertThat(entries.get(0).getSupplier()).isEqualTo("MY COMPANY");
            assertThat(entries.get(1).getSupplier()).isEqualTo("MY COMPANY");
        }

        @Test
        @DisplayName("Rows with zero debit and zero credit are skipped")
        void zeroAmountRowsSkipped() throws Exception {
            Workbook wb = new XSSFWorkbook();
            Sheet sheet = wb.createSheet();

            setCells(sheet.createRow(0), "Date", "Debit", "Credit", "Supplier");
            setCells(sheet.createRow(1), 44652.0, 1000.0, null, "A");
            setCells(sheet.createRow(2), 44653.0, 0.0, 0.0, "A"); // zero both → skip
            setCells(sheet.createRow(3), 44654.0, null, 500.0, "A");

            List<LedgerEntry> entries = parse(toBytes(wb), "test.xlsx");

            assertThat(entries).hasSize(2);
        }

        @Test
        @DisplayName("Rows with no valid date are skipped")
        void invalidDateRowsSkipped() throws Exception {
            Workbook wb = new XSSFWorkbook();
            Sheet sheet = wb.createSheet();

            setCells(sheet.createRow(0), "Date", "Debit", "Credit", "Name");
            setCells(sheet.createRow(1), 44652.0, 1000.0, null, "A");
            setCells(sheet.createRow(2), "not-a-date", 2000.0, null, "A"); // invalid date
            setCells(sheet.createRow(3), "", 3000.0, null, "A"); // empty date
            setCells(sheet.createRow(4), 44654.0, 500.0, null, "A");

            List<LedgerEntry> entries = parse(toBytes(wb), "test.xlsx");

            assertThat(entries).hasSize(2);
        }

        @Test
        @DisplayName("Tally format with Details, Vch Type columns alongside Debit/Credit → header resolution works")
        void tallyExportWithExtraColumns() throws Exception {
            Workbook wb = new XSSFWorkbook();
            Sheet sheet = wb.createSheet();

            setCells(sheet.createRow(0), "Date", "Details", "Vch Type", "Debit", "Credit", "Party Name");
            setCells(sheet.createRow(1), 44107.0, "Repair", "Journal", 3035.0, null, "A2Z Securetronix");
            setCells(sheet.createRow(2), 44174.0, "HDFC Bank", "Payment", 3035.0, null, "A2Z Securetronix");

            List<LedgerEntry> entries = parse(toBytes(wb), "TP Aircon.xlsx");

            assertThat(entries).hasSize(2);
            assertThat(entries.get(0).getSupplier()).isEqualTo("A2Z Securetronix");
            assertThat(entries.get(0).getEntryType()).isEqualTo(LedgerEntry.LedgerEntryType.PAYMENT);
        }

        @Test
        @DisplayName("Multiple suppliers in single clean file → all distinct suppliers preserved")
        void multipleSuppliersSingleFile() throws Exception {
            Workbook wb = new XSSFWorkbook();
            Sheet sheet = wb.createSheet();

            setCells(sheet.createRow(0), "Date", "Debit", "Credit", "Name");
            setCells(sheet.createRow(1), 44652.0, 1000.0, null, "Supplier A");
            setCells(sheet.createRow(2), 44653.0, null, 500.0, "Supplier B");
            setCells(sheet.createRow(3), 44654.0, 2000.0, null, "Supplier A");

            List<LedgerEntry> entries = parse(toBytes(wb), "test.xlsx");

            Map<String, List<LedgerEntry>> bySupplier = entries.stream()
                    .collect(Collectors.groupingBy(LedgerEntry::getSupplier));
            assertThat(bySupplier).hasSize(2);
            assertThat(bySupplier.get("Supplier A")).hasSize(2);
            assertThat(bySupplier.get("Supplier B")).hasSize(1);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  2. OFFSET HEADER (Metadata before header row)
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Offset Header Format (metadata rows before header)")
    class OffsetHeaderFormat {

        @Test
        @DisplayName("Header at row 5 after company metadata → detected and parsed correctly")
        void headerAtRow5() throws Exception {
            Workbook wb = new XSSFWorkbook();
            Sheet sheet = wb.createSheet();

            // 5 rows of metadata
            setCells(sheet.createRow(0), "Acme Corp Financial Report");
            setCells(sheet.createRow(1), "123 Business Street");
            setCells(sheet.createRow(2), "FY 2022-23");
            setCells(sheet.createRow(3), ""); // empty row
            setCells(sheet.createRow(4), ""); // empty row
            // Header at row 5
            setCells(sheet.createRow(5), "Date", "Debit", "Credit", "Supplier");
            setCells(sheet.createRow(6), 44652.0, 5000.0, null, "Vendor X");
            setCells(sheet.createRow(7), 44653.0, null, 3000.0, "Vendor Y");

            List<LedgerEntry> entries = parse(toBytes(wb), "test.xlsx");

            assertThat(entries).hasSize(2);
            assertThat(entries.get(0).getSupplier()).isEqualTo("Vendor X");
            assertThat(entries.get(0).getAmount().compareTo(BigDecimal.valueOf(5000.0))).isZero();
            assertThat(entries.get(1).getEntryType()).isEqualTo(LedgerEntry.LedgerEntryType.PURCHASE);
        }

        @Test
        @DisplayName("Header beyond scan depth (>20 rows) with no Ledger: markers → throws LedgerParseException")
        void headerBeyondScanDepthThrows() throws Exception {
            Workbook wb = new XSSFWorkbook();
            Sheet sheet = wb.createSheet();

            // 25 rows of garbage, then header
            for (int i = 0; i < 25; i++) {
                setCells(sheet.createRow(i), "metadata line " + i);
            }
            setCells(sheet.createRow(25), "Date", "Debit", "Credit");
            setCells(sheet.createRow(26), 44652.0, 1000.0, null);

            assertThatThrownBy(() -> parse(toBytes(wb), "test.xlsx"))
                    .isInstanceOf(LedgerParseException.class)
                    .hasMessageContaining("Could not find a header row");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  3. MULTI-LEDGER TALLY CREDITOR FORMAT
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Multi-Ledger (Tally Creditor) Format")
    class MultiLedgerFormat {

        @Test
        @DisplayName("Two supplier sections with Ledger: markers → entries have correct supplier names")
        void twoSupplierSections() throws Exception {
            Workbook wb = new XSSFWorkbook();
            Sheet sheet = wb.createSheet();

            // Company header rows
            setCells(sheet.createRow(0), "Sancus Networks 20-21", "", "", "", "", "", "", "");
            setCells(sheet.createRow(1), "D-16 Green Park", "", "", "", "", "", "", "");
            setCells(sheet.createRow(2), ""); // empty
            // Supplier 1
            setCells(sheet.createRow(3), "Ledger:", "ABHISHEK SAINI", "1-Apr-20 to 31-Mar-21");
            setCells(sheet.createRow(4), "", "95-D Shivam Enclave");
            setCells(sheet.createRow(5), "Date", "Particulars", "", "Vch Type", "Vch No.", "Debit", "Credit", "Balance");
            setCells(sheet.createRow(6), 44123.0, "To", "Icici Bank", "Payment", "fassai", 10000.0, null, 10000.0);
            setCells(sheet.createRow(7), 44131.0, "To", "Icici Bank", "Payment", "FASSAI", 4100.0, null, 14100.0);
            setCells(sheet.createRow(8), 44136.0, "By", "Professional Fees", "Journal", "1026", null, 14100.0, null);
            // Supplier 2
            setCells(sheet.createRow(9), "Ledger:", "AESTHETE INTL", "1-Apr-20 to 31-Mar-21");
            setCells(sheet.createRow(10), "", "Shop 45 Market Road");
            setCells(sheet.createRow(11), "Date", "Particulars", "", "Vch Type", "Vch No.", "Debit", "Credit", "Balance");
            setCells(sheet.createRow(12), 44200.0, "By", "Purchases 18%", "Purchase", "INV-001", null, 50000.0, 50000.0);
            setCells(sheet.createRow(13), 44250.0, "To", "HDFC Bank", "Payment", "CHQ-22", 25000.0, null, 25000.0);

            List<LedgerEntry> entries = parse(toBytes(wb), "CREDITORS 2020-21.xlsx");

            assertThat(entries).hasSize(5);

            // Supplier 1 entries
            Map<String, List<LedgerEntry>> bySupplier = entries.stream()
                    .collect(Collectors.groupingBy(LedgerEntry::getSupplier));
            assertThat(bySupplier).hasSize(2);
            assertThat(bySupplier).containsKeys("ABHISHEK SAINI", "AESTHETE INTL");

            List<LedgerEntry> saini = bySupplier.get("ABHISHEK SAINI");
            assertThat(saini).hasSize(3);
            assertThat(saini.get(0).getEntryType()).isEqualTo(LedgerEntry.LedgerEntryType.PAYMENT); // "To"
            assertThat(saini.get(0).getAmount().compareTo(BigDecimal.valueOf(10000.0))).isZero();
            assertThat(saini.get(2).getEntryType()).isEqualTo(LedgerEntry.LedgerEntryType.PURCHASE); // "By"
            assertThat(saini.get(2).getAmount().compareTo(BigDecimal.valueOf(14100.0))).isZero();

            List<LedgerEntry> aesthete = bySupplier.get("AESTHETE INTL");
            assertThat(aesthete).hasSize(2);
            assertThat(aesthete.get(0).getEntryType()).isEqualTo(LedgerEntry.LedgerEntryType.PURCHASE); // "By"
            assertThat(aesthete.get(1).getEntryType()).isEqualTo(LedgerEntry.LedgerEntryType.PAYMENT); // "To"
        }

        @Test
        @DisplayName("Supplier section with total/closing rows → total and closing rows are skipped")
        void totalAndClosingRowsSkipped() throws Exception {
            Workbook wb = new XSSFWorkbook();
            Sheet sheet = wb.createSheet();

            setCells(sheet.createRow(0), "Ledger:", "VENDOR A", "FY 2022");
            setCells(sheet.createRow(1), "Date", "Particulars", "", "Vch Type", "Vch No.", "Debit", "Credit", "Balance");
            setCells(sheet.createRow(2), 44652.0, "To", "Bank", "Payment", "1", 1000.0, null, 1000.0);
            setCells(sheet.createRow(3), 44700.0, "By", "Purchase", "Journal", "2", null, 5000.0, 4000.0);
            setCells(sheet.createRow(4), "Total", "", "", "", "", 1000.0, 5000.0, null); // should be skipped
            setCells(sheet.createRow(5), "Closing Balance", "", "", "", "", null, null, 4000.0); // skipped

            List<LedgerEntry> entries = parse(toBytes(wb), "test.xlsx");

            assertThat(entries).hasSize(2);
        }

        @Test
        @DisplayName("Ledger: marker with empty supplier name → falls back to filename")
        void emptySupplierFallsBackToFilename() throws Exception {
            Workbook wb = new XSSFWorkbook();
            Sheet sheet = wb.createSheet();

            setCells(sheet.createRow(0), "Ledger:", "", "FY 2022");
            setCells(sheet.createRow(1), "Date", "Particulars", "", "Vch Type", "Vch No.", "Debit", "Credit", "Balance");
            setCells(sheet.createRow(2), 44652.0, "To", "Bank", "Payment", "1", 1000.0, null, 1000.0);

            List<LedgerEntry> entries = parse(toBytes(wb), "COMPANY.xlsx");

            assertThat(entries).hasSize(1);
            assertThat(entries.get(0).getSupplier()).isEqualTo("COMPANY");
        }

        @Test
        @DisplayName("Opening balance rows are skipped (no real transaction)")
        void openingBalanceRowsSkipped() throws Exception {
            Workbook wb = new XSSFWorkbook();
            Sheet sheet = wb.createSheet();

            setCells(sheet.createRow(0), "Ledger:", "VENDOR X", "FY 2023");
            setCells(sheet.createRow(1), "Date", "Particulars", "", "Vch Type", "Vch No.", "Debit", "Credit", "Balance");
            setCells(sheet.createRow(2), 45017.0, "By", "Opening Balance", "", "", null, 84887.0, null);
            setCells(sheet.createRow(3), 45100.0, "To", "HDFC", "Payment", "CHQ", 10000.0, null, null);

            List<LedgerEntry> entries = parse(toBytes(wb), "test.xlsx");

            // Row 2 has "Opening Balance" in the description column (col C).
            // isNonTransactionRow() scans ALL columns, so this row is now correctly filtered.
            // Only the genuine payment row (row 3) should survive.
            assertThat(entries).hasSize(1);
            assertThat(entries.get(0).getEntryType()).isEqualTo(LedgerEntry.LedgerEntryType.PAYMENT);
            assertThat(entries.get(0).getAmount().compareTo(BigDecimal.valueOf(10000.0))).isZero();
        }

        @Test
        @DisplayName("Multiple sections — some with no data rows → those sections silently skipped")
        void emptySupplierSectionSkipped() throws Exception {
            Workbook wb = new XSSFWorkbook();
            Sheet sheet = wb.createSheet();

            // Section 1 — with data
            setCells(sheet.createRow(0), "Ledger:", "VENDOR A", "FY 2022");
            setCells(sheet.createRow(1), "Date", "Particulars", "", "Vch Type", "Vch No.", "Debit", "Credit", "Balance");
            setCells(sheet.createRow(2), 44652.0, "To", "Bank", "Payment", "1", 1000.0, null, null);
            // Section 2 — all rows have no valid data
            setCells(sheet.createRow(3), "Ledger:", "VENDOR B", "FY 2022");
            setCells(sheet.createRow(4), "Date", "Particulars", "", "Vch Type", "Vch No.", "Debit", "Credit", "Balance");
            setCells(sheet.createRow(5), "", "", "", "", "", null, null, null); // no date
            // Section 3 — with data
            setCells(sheet.createRow(6), "Ledger:", "VENDOR C", "FY 2022");
            setCells(sheet.createRow(7), "Date", "Particulars", "", "Vch Type", "Vch No.", "Debit", "Credit", "Balance");
            setCells(sheet.createRow(8), 44700.0, "By", "Purchase", "Journal", "2", null, 3000.0, null);

            List<LedgerEntry> entries = parse(toBytes(wb), "test.xlsx");

            assertThat(entries).hasSize(2);
            Map<String, List<LedgerEntry>> bySupplier = entries.stream()
                    .collect(Collectors.groupingBy(LedgerEntry::getSupplier));
            assertThat(bySupplier).containsKeys("VENDOR A", "VENDOR C");
            assertThat(bySupplier).doesNotContainKey("VENDOR B");
        }

        @Test
        @DisplayName("Supplier name with carriage return/newline → cleaned up")
        void supplierNameWithNewlinesCleaned() throws Exception {
            Workbook wb = new XSSFWorkbook();
            Sheet sheet = wb.createSheet();

            setCells(sheet.createRow(0), "Ledger:", "AARUSH ENTERPRISES\r\n", "FY 2022");
            setCells(sheet.createRow(1), "Date", "Particulars", "", "Vch Type", "Vch No.", "Debit", "Credit", "Balance");
            setCells(sheet.createRow(2), 44652.0, "To", "Bank", "Payment", "1", 5000.0, null, null);

            List<LedgerEntry> entries = parse(toBytes(wb), "test.xlsx");

            assertThat(entries).hasSize(1);
            assertThat(entries.get(0).getSupplier()).isEqualTo("AARUSH ENTERPRISES");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  4. POSITION-BASED FALLBACK
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Position-Based Fallback (4 columns, no credit header)")
    class PositionBasedFallback {

        @Test
        @DisplayName("4 columns with no credit keyword in headers → position-based parsing")
        void fourColumnsNoCredit() throws Exception {
            Workbook wb = new XSSFWorkbook();
            Sheet sheet = wb.createSheet();

            setCells(sheet.createRow(0), "Date", "Amount Out", "Amount In", "Vendor");
            setCells(sheet.createRow(1), 44652.0, 1000.0, null, "Supplier X");
            setCells(sheet.createRow(2), 44653.0, null, 500.0, "Supplier X");

            List<LedgerEntry> entries = parse(toBytes(wb), "test.xlsx");

            assertThat(entries).hasSize(2);
            assertThat(entries.get(0).getEntryType()).isEqualTo(LedgerEntry.LedgerEntryType.PAYMENT);
            assertThat(entries.get(1).getEntryType()).isEqualTo(LedgerEntry.LedgerEntryType.PURCHASE);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  5. DATE PARSING EDGE CASES
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Date Parsing — Edge Cases and Indian Formats")
    class DateParsing {

        @Test
        @DisplayName("Excel serial number (44652) → 2022-04-01")
        void excelSerialNumber() {
            LocalDate result = LedgerExcelParser.parseExcelDate(44652.0);
            assertThat(result).isEqualTo(LocalDate.of(2022, 4, 1));
        }

        @Test
        @DisplayName("Excel serial as string ('44652') → 2022-04-01")
        void excelSerialAsString() {
            LocalDate result = LedgerExcelParser.parseExcelDate("44652");
            assertThat(result).isEqualTo(LocalDate.of(2022, 4, 1));
        }

        @Test
        @DisplayName("Excel serial with decimal ('44652.0') → 2022-04-01")
        void excelSerialWithDecimal() {
            LocalDate result = LedgerExcelParser.parseExcelDate("44652.0");
            assertThat(result).isEqualTo(LocalDate.of(2022, 4, 1));
        }

        @Test
        @DisplayName("Indian format '1-Apr-20' → 2020-04-01")
        void indianFormatDashMonthShortYear() {
            LocalDate result = LedgerExcelParser.parseExcelDate("1-Apr-20");
            assertThat(result).isEqualTo(LocalDate.of(2020, 4, 1));
        }

        @Test
        @DisplayName("Indian format '15-Jan-2023' → 2023-01-15")
        void indianFormatDashMonthFullYear() {
            LocalDate result = LedgerExcelParser.parseExcelDate("15-Jan-2023");
            assertThat(result).isEqualTo(LocalDate.of(2023, 1, 15));
        }

        @Test
        @DisplayName("DD-MM-YYYY '25-12-2022' → 2022-12-25")
        void ddMmYyyyFormat() {
            LocalDate result = LedgerExcelParser.parseExcelDate("25-12-2022");
            assertThat(result).isEqualTo(LocalDate.of(2022, 12, 25));
        }

        @Test
        @DisplayName("DD/MM/YYYY '25/12/2022' → 2022-12-25")
        void ddMmYyyySlashFormat() {
            LocalDate result = LedgerExcelParser.parseExcelDate("25/12/2022");
            assertThat(result).isEqualTo(LocalDate.of(2022, 12, 25));
        }

        @Test
        @DisplayName("ISO-8601 '2022-04-01' → 2022-04-01")
        void isoFormat() {
            LocalDate result = LedgerExcelParser.parseExcelDate("2022-04-01");
            assertThat(result).isEqualTo(LocalDate.of(2022, 4, 1));
        }

        @Test
        @DisplayName("Null → null")
        void nullReturnsNull() {
            assertThat(LedgerExcelParser.parseExcelDate(null)).isNull();
        }

        @Test
        @DisplayName("Empty string → null")
        void emptyStringReturnsNull() {
            assertThat(LedgerExcelParser.parseExcelDate("")).isNull();
        }

        @Test
        @DisplayName("Gibberish text → null")
        void gibberishReturnsNull() {
            assertThat(LedgerExcelParser.parseExcelDate("not-a-date-at-all")).isNull();
        }

        @Test
        @DisplayName("LocalDate instance passed through directly")
        void localDatePassThrough() {
            LocalDate date = LocalDate.of(2023, 6, 15);
            assertThat(LedgerExcelParser.parseExcelDate(date)).isEqualTo(date);
        }

        @Test
        @DisplayName("java.util.Date → converted to LocalDate")
        void javaUtilDateConverted() {
            java.util.Date d = java.util.Date.from(
                    LocalDate.of(2023, 6, 15).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
            assertThat(LedgerExcelParser.parseExcelDate(d)).isEqualTo(LocalDate.of(2023, 6, 15));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  6. ERROR CASES
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("Empty sheet → LedgerParseException")
        void emptySheetThrows() throws Exception {
            Workbook wb = new XSSFWorkbook();
            wb.createSheet(); // empty sheet with no rows

            assertThatThrownBy(() -> parse(toBytes(wb), "empty.xlsx"))
                    .isInstanceOf(LedgerParseException.class)
                    .hasMessageContaining("empty");
        }

        @Test
        @DisplayName("No Date column found → descriptive error with found headers")
        void noDateColumnThrows() throws Exception {
            Workbook wb = new XSSFWorkbook();
            Sheet sheet = wb.createSheet();

            setCells(sheet.createRow(0), "Amount", "Type", "Reference", "Status", "Notes");
            setCells(sheet.createRow(1), 1000.0, "Payment", "REF-001", "Done", "");

            assertThatThrownBy(() -> parse(toBytes(wb), "test.xlsx"))
                    .isInstanceOf(LedgerParseException.class)
                    .hasMessageContaining("Could not find a header row");
        }

        @Test
        @DisplayName("Date column but no Debit/Credit columns → LedgerParseException")
        void noDebitCreditThrows() throws Exception {
            Workbook wb = new XSSFWorkbook();
            Sheet sheet = wb.createSheet();

            setCells(sheet.createRow(0), "Date", "Description", "Reference", "Type", "Category");
            setCells(sheet.createRow(1), 44652.0, "Invoice", "INV-001", "Sales", "General");

            assertThatThrownBy(() -> parse(toBytes(wb), "test.xlsx"))
                    .isInstanceOf(LedgerParseException.class)
                    .hasMessageContaining("Debit or Credit");
        }

        @Test
        @DisplayName("Multi-ledger file with all suppliers having no valid data → LedgerParseException")
        void multiLedgerAllEmptyThrows() throws Exception {
            Workbook wb = new XSSFWorkbook();
            Sheet sheet = wb.createSheet();

            setCells(sheet.createRow(0), "Ledger:", "VENDOR A", "FY 2022");
            setCells(sheet.createRow(1), "Date", "Particulars", "", "Vch Type", "Vch No.", "Debit", "Credit", "Balance");
            setCells(sheet.createRow(2), "", "", "", "", "", null, null, null); // no date = skip

            assertThatThrownBy(() -> parse(toBytes(wb), "test.xlsx"))
                    .isInstanceOf(LedgerParseException.class)
                    .hasMessageContaining("No valid entries");
        }

        @Test
        @DisplayName("Multi-ledger with section missing sub-header → section skipped, rest parsed")
        void sectionWithoutSubHeaderSkipped() throws Exception {
            Workbook wb = new XSSFWorkbook();
            Sheet sheet = wb.createSheet();

            // Section 1 — missing sub-header
            setCells(sheet.createRow(0), "Ledger:", "VENDOR A", "FY 2022");
            setCells(sheet.createRow(1), "Some random text without date header");
            setCells(sheet.createRow(2), 44652.0, "To", "Bank", "Payment", "1", 1000.0, null, null);
            // Section 2 — valid
            setCells(sheet.createRow(3), "Ledger:", "VENDOR B", "FY 2022");
            setCells(sheet.createRow(4), "Date", "Particulars", "", "Vch Type", "Vch No.", "Debit", "Credit", "Balance");
            setCells(sheet.createRow(5), 44700.0, "To", "Bank", "Payment", "2", 2000.0, null, null);

            List<LedgerEntry> entries = parse(toBytes(wb), "test.xlsx");

            assertThat(entries).hasSize(1);
            assertThat(entries.get(0).getSupplier()).isEqualTo("VENDOR B");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  7. LEDGER COUNT (countLedgers interface method)
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Ledger Count — Distinct Supplier Counting")
    class LedgerCount {

        @Test
        @DisplayName("Multi-supplier file → countLedgers returns distinct supplier count")
        void countDistinctSuppliers() throws Exception {
            Workbook wb = new XSSFWorkbook();
            Sheet sheet = wb.createSheet();

            setCells(sheet.createRow(0), "Ledger:", "VENDOR A", "FY 2022");
            setCells(sheet.createRow(1), "Date", "Particulars", "", "Vch Type", "Vch No.", "Debit", "Credit", "Balance");
            setCells(sheet.createRow(2), 44652.0, "To", "Bank", "Payment", "1", 1000.0, null, null);
            setCells(sheet.createRow(3), "Ledger:", "VENDOR B", "FY 2022");
            setCells(sheet.createRow(4), "Date", "Particulars", "", "Vch Type", "Vch No.", "Debit", "Credit", "Balance");
            setCells(sheet.createRow(5), 44700.0, "By", "Purchase", "Journal", "2", null, 3000.0, null);
            setCells(sheet.createRow(6), "Ledger:", "VENDOR C", "FY 2022");
            setCells(sheet.createRow(7), "Date", "Particulars", "", "Vch Type", "Vch No.", "Debit", "Credit", "Balance");
            setCells(sheet.createRow(8), 44750.0, "To", "SBI", "Payment", "3", 500.0, null, null);

            List<LedgerEntry> entries = parse(toBytes(wb), "test.xlsx");

            int count = parser.countLedgers(entries);
            assertThat(count).isEqualTo(3);
        }

        @Test
        @DisplayName("Single supplier → countLedgers returns 1")
        void singleSupplierReturnsOne() throws Exception {
            Workbook wb = new XSSFWorkbook();
            Sheet sheet = wb.createSheet();

            setCells(sheet.createRow(0), "Date", "Debit", "Credit", "Name");
            setCells(sheet.createRow(1), 44652.0, 1000.0, null, "KD");
            setCells(sheet.createRow(2), 44653.0, 2000.0, null, "KD");

            List<LedgerEntry> entries = parse(toBytes(wb), "KD STEEL.xlsx");

            assertThat(parser.countLedgers(entries)).isEqualTo(1);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  8. NORMALIZE COLUMN NAME
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Column Name Normalization")
    class ColumnNameNormalization {

        @Test
        @DisplayName("Mixed case with spaces → lowercase letters only")
        void mixedCaseNormalized() {
            assertThat(LedgerExcelParser.normalizeColumnName("Vch Type")).isEqualTo("vchtype");
        }

        @Test
        @DisplayName("Date with special chars → 'date'")
        void dateWithSpecialChars() {
            assertThat(LedgerExcelParser.normalizeColumnName("Date*")).isEqualTo("date");
        }

        @Test
        @DisplayName("Null → empty string")
        void nullNormalized() {
            assertThat(LedgerExcelParser.normalizeColumnName(null)).isEmpty();
        }

        @Test
        @DisplayName("Numeric string → empty (all non-alpha stripped)")
        void numericStringNormalized() {
            assertThat(LedgerExcelParser.normalizeColumnName("12345")).isEmpty();
        }
    }
}
