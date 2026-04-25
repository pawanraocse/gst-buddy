"""
Unit tests for Gstr3bPdfExtractor — Phase C table extraction.

Tests use a purpose-built minimal PDF fixture that mimics the
current GST portal GSTR-3B format for table 3.1 and 6.1.
Where real PDF parsing is not feasible in unit tests, we test
the table-parsing logic directly by passing pre-extracted table
data structures to the private methods via the public extract()
interface (through a test subclass approach).
"""

import pytest
from unittest.mock import patch, MagicMock
from app.engines.gstr3b_pdf import Gstr3bPdfExtractor


class TestGstr3bTableExtraction:
    """Tests for Table 3.1 and Table 6.1 extraction logic."""

    def setup_method(self):
        self.extractor = Gstr3bPdfExtractor()

    # ── Table 3.1 row matching tests ─────────────────────────────────────────

    def test_parse_table_3_1_matches_outward_taxable_row(self):
        """Table 3.1 row (a) — outward taxable supplies — is correctly identified."""
        tables = [[
            ["(a) Outward taxable supplies (other than zero rated", "100000", "18000", "9000", "9000", "0"],
            ["(b) Outward taxable supplies (zero rated", "50000", "9000", "0", "0", "0"],
        ]]
        result = self.extractor._parse_table_3_1(tables)
        assert result is not None
        assert "outward_taxable" in result
        assert result["outward_taxable"]["taxable_value"] == 100000.0
        assert result["outward_taxable"]["igst"] == 18000.0
        assert result["outward_taxable"]["cgst"] == 9000.0
        assert result["outward_taxable"]["sgst_utgst"] == 9000.0
        assert result["outward_taxable"]["cess"] == 0.0

    def test_parse_table_3_1_nil_exempted_row_has_only_taxable_value(self):
        """Row (c) — nil/exempted — should only have taxable_value, no tax cols."""
        tables = [[
            ["(a) Outward taxable supplies (other than zero rated", "100000", "18000", "9000", "9000", "0"],
            ["(c) Other outward supplies (nil rated, exempted", "25000", "", "", "", ""],
        ]]
        result = self.extractor._parse_table_3_1(tables)
        assert result is not None
        assert "outward_nil_exempted" in result
        assert result["outward_nil_exempted"]["taxable_value"] == 25000.0
        assert "igst" not in result["outward_nil_exempted"]

    def test_parse_table_3_1_returns_none_for_unrecognised_table(self):
        """Non-3.1 table should return None without raising."""
        tables = [[
            ["ITC Details", "Central Tax", "State Tax"],
            ["ITC Available", "10000", "10000"],
        ]]
        result = self.extractor._parse_table_3_1(tables)
        assert result is None

    # ── Table 6.1 tests ──────────────────────────────────────────────────────

    def test_parse_table_6_1_extracts_tax_payable_row(self):
        """Table 6.1 'total tax payable' row correctly extracted."""
        tables = [[
            ["Description", "IGST", "CGST", "SGST/UTGST", "CESS"],
            ["Total tax payable", "18000", "9000", "9000", "0"],
            ["Paid through ITC", "10000", "5000", "5000", "0"],
            ["Tax paid in cash", "8000", "4000", "4000", "0"],
        ]]
        result = self.extractor._parse_table_6_1(tables)
        assert result is not None
        assert "tax_payable" in result
        assert result["tax_payable"]["igst"] == 18000.0
        assert result["tax_payable"]["cgst"] == 9000.0
        assert result["tax_payable"]["sgst_utgst"] == 9000.0
        assert "paid_through_itc" in result
        assert result["paid_through_itc"]["igst"] == 10000.0
        assert "paid_in_cash" in result
        assert result["paid_in_cash"]["igst"] == 8000.0

    def test_parse_table_6_1_returns_none_for_unrecognised_table(self):
        """Non-6.1 table returns None without raising."""
        tables = [[
            ["Some other table", "Col1"],
            ["Row data", "Value"],
        ]]
        result = self.extractor._parse_table_6_1(tables)
        assert result is None

    # ── Amount parsing utility ───────────────────────────────────────────────

    def test_parse_amount_handles_comma_formatted_numbers(self):
        """Comma-formatted GST portal amounts parsed correctly."""
        assert Gstr3bPdfExtractor._parse_amount("1,00,000.00") == 100000.0
        assert Gstr3bPdfExtractor._parse_amount("9,000") == 9000.0
        assert Gstr3bPdfExtractor._parse_amount("0") == 0.0

    def test_parse_amount_returns_none_for_non_numeric(self):
        """Non-numeric strings return None."""
        assert Gstr3bPdfExtractor._parse_amount("") is None
        assert Gstr3bPdfExtractor._parse_amount("N/A") is None
        assert Gstr3bPdfExtractor._parse_amount(None) is None

    # ── Warning on unsupported format ────────────────────────────────────────

    def test_warning_added_when_tables_not_found(self):
        """Warning is added when table 3.1 cannot be found in the PDF tables."""
        with patch("pdfplumber.open") as mock_open:
            mock_pdf = MagicMock()
            mock_page = MagicMock()
            mock_page.extract_text.return_value = ""
            mock_page.extract_tables.return_value = []  # no tables
            mock_pdf.pages = [mock_page]
            mock_open.return_value.__enter__.return_value = mock_pdf

            result = self.extractor.extract(b"fake-pdf-bytes")
            warnings = self.extractor.get_warnings()
            assert any("No tables found" in w for w in warnings)
