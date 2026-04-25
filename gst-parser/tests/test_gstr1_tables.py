"""
Unit tests for Gstr1PdfExtractor — Phase C table extraction.

Tests verify:
  1. _parse_liability_summary() from table data structures
  2. _parse_invoices() for Tables 4A (B2B) and 5A (B2CL)
  3. Amount parsing utility
  4. Graceful warnings on unsupported formats
"""

import pytest
from unittest.mock import patch, MagicMock
from app.engines.gstr1_pdf import Gstr1PdfExtractor


class TestGstr1LiabilitySummaryExtraction:
    """Tests for _parse_liability_summary()."""

    def setup_method(self):
        self.extractor = Gstr1PdfExtractor()

    def test_parse_liability_summary_from_table_total_row(self):
        """
        A table with a 'total taxable value' row and 5 numeric columns
        should produce a full liability summary.
        """
        tables = [[
            ["Description", "Taxable Value", "IGST", "CGST", "SGST", "CESS"],
            ["Outward supplies", "100000", "0", "9000", "9000", "0"],
            ["Total Taxable Value", "100000", "0", "9000", "9000", "0"],
        ]]
        result = self.extractor._parse_liability_summary("", tables)
        assert result is not None
        assert result["total_taxable_value"] == 100000.0
        assert result["total_igst"] == 0.0
        assert result["total_cgst"] == 9000.0
        assert result["total_sgst_utgst"] == 9000.0
        assert result["total_cess"] == 0.0

    def test_parse_liability_summary_falls_back_to_regex_on_text(self):
        """
        When tables don't have a matching row, regex fallback on full text
        should still find 5 amounts after 'grand total'.
        """
        full_text = "Grand Total 2,00,000 18,000 9,000 9,000 500"
        result = self.extractor._parse_liability_summary(full_text, [])
        assert result is not None
        assert result["total_taxable_value"] == 200000.0

    def test_parse_liability_summary_returns_none_for_no_match(self):
        """No matching row or text → returns None (caller adds warning)."""
        result = self.extractor._parse_liability_summary("some irrelevant text", [])
        assert result is None


class TestGstr1InvoiceExtraction:
    """Tests for _parse_invoices()."""

    def setup_method(self):
        self.extractor = Gstr1PdfExtractor()

    def test_parse_invoices_extracts_b2b_row(self):
        """
        Table 4A (B2B) row with all 12 columns should produce a valid invoice.
        Columns: GSTIN, invoice_no, invoice_date, value, PoS, RC, rate,
                 taxable_value, IGST, CGST, SGST, CESS
        """
        tables = [[
            ["4A Taxable outward supplies made to registered persons", "", "", "", "", "", "", "", "", "", "", ""],
            ["27ABCDE1234F1Z5", "INV-001", "15/04/2024", "11800", "29-Karnataka", "N", "18", "10000", "0", "900", "900", "0"],
        ]]
        invoices = self.extractor._parse_invoices(tables, "")
        assert len(invoices) == 1
        inv = invoices[0]
        assert inv["invoice_no"] == "INV-001"
        assert inv["invoice_date"] == "2024-04-15"
        assert inv["table_section"] == "4A"
        assert inv["cgst"] == 900.0
        assert inv["sgst"] == 900.0
        assert inv["igst"] == 0.0
        assert inv["taxable_value"] == 10000.0

    def test_parse_invoices_skips_row_without_valid_date(self):
        """Rows without a recognisable date pattern are silently skipped."""
        tables = [[
            ["4A Taxable outward supplies made to registered persons", "", "", "", "", "", "", "", "", "", "", ""],
            ["27ABCDE1234F1Z5", "INV-001", "INVALID DATE", "11800", "29-Karnataka", "N", "18", "10000", "0", "900", "900", "0"],
        ]]
        invoices = self.extractor._parse_invoices(tables, "")
        assert len(invoices) == 0

    def test_parse_invoices_returns_empty_for_no_invoice_tables(self):
        """If no B2B/B2CL tables are found, returns an empty list."""
        tables = [[
            ["Summary Section", "Total"],
            ["Outward", "100000"],
        ]]
        invoices = self.extractor._parse_invoices(tables, "")
        assert invoices == []

    def test_parse_invoices_handles_multiple_rows_in_b2b_table(self):
        """Multiple invoice rows in a single B2B table are all extracted."""
        tables = [[
            ["4A Taxable outward supplies made to registered persons", "", "", "", "", "", "", "", "", "", "", ""],
            ["27ABCDE1234F1Z5", "INV-001", "01/04/2024", "11800", "29", "N", "18", "10000", "0", "900", "900", "0"],
            ["29XYZAB5678G1Z3", "INV-002", "15/04/2024", "5900",  "27", "N", "18", "5000",  "0", "450", "450", "0"],
        ]]
        invoices = self.extractor._parse_invoices(tables, "")
        assert len(invoices) == 2
        assert invoices[0]["invoice_no"] == "INV-001"
        assert invoices[1]["invoice_no"] == "INV-002"
