import io
import re
import logging
from typing import Dict, Any, List, Optional

import pdfplumber

from app.engines.base import BaseExtractor
from app.validators.dates import normalize_date

logger = logging.getLogger(__name__)


class Gstr1PdfExtractor(BaseExtractor):
    """
    Extracts metadata and table data from Government GSTR-1 PDFs.

    Default backend: pdfplumber (precise character-level coordinate extraction).
    Fallback:        PyMuPDF (fitz) — used only when pdfplumber fails to open the PDF.

    Extracted fields:
      Metadata (page 1):
        - gstin          : 15-char taxpayer GSTIN
        - arn            : Acknowledgement Reference Number
        - arn_date       : Filing date (ISO 8601) — CRITICAL for late fee computation
        - tax_period     : e.g. "March" / "April"
        - financial_year : e.g. "2024-25"

      Table data (Phase C):
        - liability_summary : Aggregated outward liability totals
            {
              "total_taxable_value": float,
              "total_igst":          float,
              "total_cgst":          float,
              "total_sgst_utgst":    float,
              "total_cess":          float
            }
        - invoices : Per-invoice rows from Tables 4A (B2B) and 5A (B2C Large).
            [
              {
                "table_section": "4A",
                "invoice_no":    "INV-001",
                "invoice_date":  "2024-04-15",
                "place_of_supply": "29-Karnataka",
                "taxable_value": float,
                "cgst":  float,
                "sgst":  float,
                "igst":  float,
                "cess":  float,
                "rate":  float
              }
            ]

    Unsupported format: if tables cannot be matched, a warning is added and the
    field is omitted. Callers must check for field presence before using.
    """

    # Table section identifiers in current GST portal GSTR-1 PDFs
    _B2B_SECTION_MARKERS   = ["4a", "4 a", "b2b", "taxable outward supplies made to registered persons"]
    _B2CL_SECTION_MARKERS  = ["5a", "5 a", "b2cl", "taxable outward inter-state supplies to un-registered persons"]

    # Summary section markers
    _SUMMARY_MARKERS = ["total taxable value", "total liability", "grand total", "aggregate value"]

    def extract(self, content: bytes, json_data: Dict[str, Any] = None) -> Dict[str, Any]:
        result = self._extract_with_pdfplumber(content)

        if not result.get("gstin") and not result.get("arn_date"):
            logger.warning("pdfplumber returned empty result for GSTR-1; attempting PyMuPDF fallback.")
            fallback = self._extract_with_pymupdf(content)
            if fallback:
                result = fallback

        return result

    # ── Primary: pdfplumber ──────────────────────────────────────────────────

    def _extract_with_pdfplumber(self, content: bytes) -> Dict[str, Any]:
        result: Dict[str, Any] = {}
        try:
            with pdfplumber.open(io.BytesIO(content)) as pdf:
                if not pdf.pages:
                    self.add_warning("GSTR-1 PDF has no pages.")
                    return result

                # Page 1: metadata
                text = pdf.pages[0].extract_text() or ""
                result = self._parse_text(text)

                # All pages: collect tables for table extraction
                all_tables: List[List[List[str]]] = []
                full_text = ""
                for page in pdf.pages:
                    page_text = page.extract_text() or ""
                    full_text += "\n" + page_text
                    page_tables = page.extract_tables()
                    if page_tables:
                        all_tables.extend(page_tables)

                # Extract liability summary from text (summary section at end of PDF)
                summary = self._parse_liability_summary(full_text, all_tables)
                if summary:
                    result["liability_summary"] = summary
                else:
                    self.add_warning(
                        "GSTR-1 liability summary not found or unsupported PDF format. "
                        "Upload the latest version downloaded from the GST portal."
                    )

                # Extract per-invoice rows (Tables 4A + 5A)
                invoices = self._parse_invoices(all_tables, full_text)
                result["invoices"] = invoices  # always present — may be empty list
                if not invoices:
                    self.add_warning(
                        "No invoice rows extracted from GSTR-1. "
                        "LATE_REPORTING_GSTR1 rule requires per-invoice data."
                    )

        except Exception as e:
            self.add_warning(f"pdfplumber error for GSTR-1: {str(e)}")

        return result

    # ── Fallback: PyMuPDF ────────────────────────────────────────────────────

    def _extract_with_pymupdf(self, content: bytes) -> Optional[Dict[str, Any]]:
        try:
            import fitz
            doc = fitz.open("pdf", content)
            if not doc:
                return None
            text = doc[0].get_text()
            doc.close()
            return self._parse_text(text)
        except Exception as e:
            self.add_warning(f"PyMuPDF fallback also failed for GSTR-1: {str(e)}")
            return None

    # ── Metadata parsing ─────────────────────────────────────────────────────

    def _parse_text(self, text: str) -> Dict[str, Any]:
        result: Dict[str, Any] = {}

        # 1. GSTIN
        gstin_match = re.search(r"GSTIN[^\n]*\n([0-9A-Z]{15})", text)
        if not gstin_match:
            gstin_match = re.search(r"\b([0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[0-9A-Z]{1}[Z]{1}[0-9A-Z]{1})\b", text)
        if gstin_match:
            result["gstin"] = gstin_match.group(1)

        # 2. ARN
        arn_match = re.search(r"ARN[^\n]*\n([A-Z0-9]+)", text)
        if arn_match:
            result["arn"] = arn_match.group(1)

        # 3. ARN Date — CRITICAL for late fee computation (Section 47(1), CGST Act 2017)
        arn_date_match = re.search(
            r"(?:ARN date|Date of ARN|Date of filing)[^\n]*\n(\d{2}[/\-]\d{2}[/\-]\d{4})",
            text, re.IGNORECASE
        )
        if arn_date_match:
            normalized = normalize_date(arn_date_match.group(1))
            if normalized:
                result["arn_date"] = normalized
            else:
                self.add_warning(f"Failed to normalize ARN Date: {arn_date_match.group(1)}")

        # 4. Tax Period
        period_match = re.search(r"Tax period\n(\w+)", text)
        if period_match:
            result["tax_period"] = period_match.group(1).strip()

        # 5. Financial Year
        year_match = re.search(r"Financial year\n(\d{4}-\d{2})", text)
        if year_match:
            result["financial_year"] = year_match.group(1).strip()

        return result

    # ── Liability Summary ─────────────────────────────────────────────────────

    def _parse_liability_summary(
        self, full_text: str, tables: List[List[List[str]]]
    ) -> Optional[Dict[str, Any]]:
        """
        Extract aggregate liability totals from GSTR-1.
        Strategy 1: find a summary row in the extracted tables.
        Strategy 2: regex fallback on raw text.
        """
        # Strategy 1: table scan for a "total" row with 5 numeric columns
        for table in tables:
            if not table:
                continue
            for row in table:
                if not row:
                    continue
                row_text = " ".join(str(c).lower().strip() if c else "" for c in row)
                if any(m in row_text for m in self._SUMMARY_MARKERS):
                    nums = [self._parse_amount(str(c)) for c in row]
                    numeric = [v for v in nums if v is not None]
                    if len(numeric) >= 5:
                        return {
                            "total_taxable_value": numeric[0],
                            "total_igst":          numeric[1],
                            "total_cgst":          numeric[2],
                            "total_sgst_utgst":    numeric[3],
                            "total_cess":          numeric[4],
                        }
                    elif len(numeric) >= 4:
                        return {
                            "total_taxable_value": numeric[0],
                            "total_igst":          numeric[1],
                            "total_cgst":          numeric[2],
                            "total_sgst_utgst":    numeric[3],
                            "total_cess":          0.0,
                        }

        # Strategy 2: regex on text — look for 5 consecutive amounts after a summary label
        amount_pattern = r"[\d,]+\.?\d*"
        for marker in self._SUMMARY_MARKERS:
            match = re.search(
                rf"{re.escape(marker)}.*?({amount_pattern})\s+({amount_pattern})\s+({amount_pattern})\s+({amount_pattern})\s+({amount_pattern})",
                full_text, re.IGNORECASE | re.DOTALL
            )
            if match:
                try:
                    return {
                        "total_taxable_value": self._parse_amount(match.group(1)),
                        "total_igst":          self._parse_amount(match.group(2)),
                        "total_cgst":          self._parse_amount(match.group(3)),
                        "total_sgst_utgst":    self._parse_amount(match.group(4)),
                        "total_cess":          self._parse_amount(match.group(5)),
                    }
                except Exception:
                    continue

        return None

    # ── Per-Invoice Extraction ────────────────────────────────────────────────

    def _parse_invoices(
        self, tables: List[List[List[str]]], full_text: str
    ) -> List[Dict[str, Any]]:
        """
        Extract per-invoice rows from Tables 4A (B2B) and 5A (B2CL).
        Each row must have: invoice_no, invoice_date, place_of_supply, taxable_value, tax amounts.
        Returns empty list if no invoice tables are found.
        """
        invoices: List[Dict[str, Any]] = []
        current_section: Optional[str] = None

        for table in tables:
            if not table or len(table) < 2:
                continue

            # Detect which section this table belongs to
            header_text = " ".join(
                str(c).lower().strip() if c else ""
                for c in (table[0] or [])
            )

            section = None
            if any(m in header_text for m in self._B2B_SECTION_MARKERS):
                section = "4A"
            elif any(m in header_text for m in self._B2CL_SECTION_MARKERS):
                section = "5A"
            elif current_section and self._looks_like_invoice_row(table[0]):
                # Continuation table (same section, no header repeated)
                section = current_section

            if not section:
                current_section = None
                continue

            current_section = section
            # Skip header row(s)
            start_row = 1
            if len(table) > 1 and not self._looks_like_invoice_row(table[1]):
                start_row = 2  # double-header tables

            for row in table[start_row:]:
                invoice = self._parse_invoice_row(row, section)
                if invoice:
                    invoices.append(invoice)

        return invoices

    def _looks_like_invoice_row(self, row: Optional[List]) -> bool:
        if not row:
            return False
        # An invoice row has a date-like string somewhere
        row_text = " ".join(str(c) if c else "" for c in row)
        return bool(re.search(r"\d{2}[/\-]\d{2}[/\-]\d{4}", row_text))

    def _parse_invoice_row(self, row: List, section: str) -> Optional[Dict[str, Any]]:
        """
        B2B (4A) columns (current portal format):
          [0] GSTIN of Recipient  [1] Invoice No  [2] Invoice Date
          [3] Invoice Value  [4] Place of Supply  [5] Reverse Charge
          [6] Rate  [7] Taxable Value  [8] IGST  [9] CGST  [10] SGST/UTGST  [11] CESS

        B2CL (5A) columns:
          [0] Invoice No  [1] Invoice Date  [2] Invoice Value
          [3] Place of Supply  [4] Rate  [5] Taxable Value
          [6] IGST  [7] CESS
        """
        if not row or len(row) < 6:
            return None

        row_str = [str(c).strip() if c else "" for c in row]

        try:
            if section == "4A":
                invoice_no   = row_str[1] if len(row_str) > 1 else ""
                raw_date     = row_str[2] if len(row_str) > 2 else ""
                pos          = row_str[4] if len(row_str) > 4 else ""
                rate         = self._parse_amount(row_str[6]) if len(row_str) > 6 else None
                taxable      = self._parse_amount(row_str[7]) if len(row_str) > 7 else None
                igst         = self._parse_amount(row_str[8]) if len(row_str) > 8 else 0.0
                cgst         = self._parse_amount(row_str[9]) if len(row_str) > 9 else 0.0
                sgst         = self._parse_amount(row_str[10]) if len(row_str) > 10 else 0.0
                cess         = self._parse_amount(row_str[11]) if len(row_str) > 11 else 0.0
            else:  # 5A B2CL
                invoice_no   = row_str[0]
                raw_date     = row_str[1] if len(row_str) > 1 else ""
                pos          = row_str[3] if len(row_str) > 3 else ""
                rate         = self._parse_amount(row_str[4]) if len(row_str) > 4 else None
                taxable      = self._parse_amount(row_str[5]) if len(row_str) > 5 else None
                igst         = self._parse_amount(row_str[6]) if len(row_str) > 6 else 0.0
                cgst         = 0.0  # B2CL is always inter-state → IGST only
                sgst         = 0.0
                cess         = self._parse_amount(row_str[7]) if len(row_str) > 7 else 0.0

            # Skip rows without a recognisable date or invoice number
            if not invoice_no or not raw_date:
                return None

            normalized_date = normalize_date(raw_date)
            if not normalized_date:
                return None  # not a valid invoice row

            if taxable is None:
                return None

            return {
                "table_section":    section,
                "invoice_no":       invoice_no,
                "invoice_date":     normalized_date,
                "place_of_supply":  pos,
                "taxable_value":    taxable or 0.0,
                "cgst":             cgst or 0.0,
                "sgst":             sgst or 0.0,
                "igst":             igst or 0.0,
                "cess":             cess or 0.0,
                "rate":             rate or 0.0,
            }
        except (IndexError, TypeError, ValueError):
            return None

    # ── Utility ──────────────────────────────────────────────────────────────

    @staticmethod
    def _parse_amount(value: str) -> Optional[float]:
        """Parse a GST portal amount string to float. Returns None if not numeric."""
        if not value:
            return None
        cleaned = value.replace(",", "").replace("₹", "").strip()
        try:
            return float(cleaned)
        except ValueError:
            return None
