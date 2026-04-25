import io
import re
import logging
from typing import Dict, Any, List, Optional

import pdfplumber

from app.engines.base import BaseExtractor
from app.validators.dates import normalize_date

logger = logging.getLogger(__name__)


class Gstr3bPdfExtractor(BaseExtractor):
    """
    Extracts metadata and table data from Government GSTR-3B PDFs.

    Default backend: pdfplumber (precise character-level coordinate extraction,
                     superior for GSTR-3B's multi-section table layout).
    Fallback:        PyMuPDF (fitz) — used only when pdfplumber fails to open the PDF.

    Extracted fields:
      Metadata (page 1):
        - gstin          : 15-char taxpayer GSTIN
        - arn            : Acknowledgement Reference Number
        - arn_date       : Filing date (ISO 8601) — CRITICAL for Section 47(2) late fee
        - tax_period     : e.g. "March" / "April"
        - financial_year : e.g. "2024-25"

      Table data (Phase C):
        - table_3_1      : Outward supply taxable summary (Section 3.1)
            {
              "outward_taxable"      : {taxable_value, igst, cgst, sgst_utgst, cess},
              "outward_taxable_zero" : {taxable_value, igst, cgst, sgst_utgst, cess},
              "outward_nil_exempted" : {taxable_value},
              "inward_rcm"           : {taxable_value, igst, cgst, sgst_utgst, cess},
              "outward_non_gst"      : {taxable_value}
            }
        - table_6_1      : Tax payment summary (Section 6.1) — used for RECON_1_VS_3B
            {
              "tax_payable"      : {igst, cgst, sgst_utgst, cess},
              "paid_through_itc" : {igst, cgst, sgst_utgst, cess},
              "paid_in_cash"     : {igst, cgst, sgst_utgst, cess}
            }

    Unsupported format: if table headers cannot be matched, a warning is added and
    the table field is omitted — callers must check for its presence.
    """

    # ── Table 3.1 row labels as they appear in current GST portal PDFs ──────
    _TABLE_3_1_ROWS = {
        "outward_taxable":      ["(a)", "outward taxable supplies (other than zero rated"],
        "outward_taxable_zero": ["(b)", "outward taxable supplies (zero rated"],
        "outward_nil_exempted": ["(c)", "other outward supplies (nil rated"],
        "inward_rcm":           ["(d)", "inward supplies (liable to reverse charge"],
        "outward_non_gst":      ["(e)", "non-gst outward supplies"],
    }

    # ── Table 6.1 row labels ─────────────────────────────────────────────────
    _TABLE_6_1_ROWS = {
        "tax_payable":      ["total tax payable", "tax payable"],
        "paid_through_itc": ["paid through itc", "through itc"],
        "paid_in_cash":     ["tax paid in cash", "paid in cash"],
    }

    def extract(self, content: bytes, json_data: Dict[str, Any] = None) -> Dict[str, Any]:
        result = self._extract_with_pdfplumber(content)

        # Fallback when pdfplumber returns empty (e.g. scanned/protected PDFs)
        if not result.get("gstin") and not result.get("arn_date"):
            logger.warning("pdfplumber returned empty result for GSTR-3B; attempting PyMuPDF fallback.")
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
                    self.add_warning("GSTR-3B PDF has no pages.")
                    return result

                # Page 1: metadata
                text = pdf.pages[0].extract_text() or ""
                result = self._parse_text(text)

                # Collect raw tables from ALL pages (3B tables may span pages)
                all_tables: List[List[List[str]]] = []
                for page in pdf.pages:
                    page_tables = page.extract_tables()
                    if page_tables:
                        all_tables.extend(page_tables)

                if all_tables:
                    table_3_1 = self._parse_table_3_1(all_tables)
                    if table_3_1:
                        result["table_3_1"] = table_3_1
                    else:
                        self.add_warning(
                            "GSTR-3B Table 3.1 not found or unsupported PDF format. "
                            "Upload the latest version downloaded from the GST portal."
                        )

                    table_6_1 = self._parse_table_6_1(all_tables)
                    if table_6_1:
                        result["table_6_1"] = table_6_1
                    else:
                        self.add_warning(
                            "GSTR-3B Table 6.1 not found or unsupported PDF format. "
                            "Upload the latest version downloaded from the GST portal."
                        )
                else:
                    self.add_warning("No tables found in GSTR-3B PDF.")

        except Exception as e:
            self.add_warning(f"pdfplumber error for GSTR-3B: {str(e)}")

        return result

    # ── Fallback: PyMuPDF ────────────────────────────────────────────────────

    def _extract_with_pymupdf(self, content: bytes) -> Optional[Dict[str, Any]]:
        try:
            import fitz  # PyMuPDF — optional fallback
            doc = fitz.open("pdf", content)
            if not doc:
                return None
            text = doc[0].get_text()
            doc.close()
            return self._parse_text(text)
        except Exception as e:
            self.add_warning(f"PyMuPDF fallback also failed for GSTR-3B: {str(e)}")
            return None

    # ── Shared metadata parsing ──────────────────────────────────────────────

    def _parse_text(self, text: str) -> Dict[str, Any]:
        result: Dict[str, Any] = {}

        # 1. GSTIN
        gstin_match = re.search(r"GSTIN[^\n]*\n([0-9A-Z]{15})", text, re.IGNORECASE)
        if not gstin_match:
            gstin_match = re.search(r"\b([0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[0-9A-Z]{1}[Z]{1}[0-9A-Z]{1})\b", text)
        if gstin_match:
            result["gstin"] = gstin_match.group(1)

        # 2. ARN
        arn_match = re.search(r"ARN\n([A-Z0-9]+)", text)
        if arn_match:
            result["arn"] = arn_match.group(1)

        # 3. ARN Date — CRITICAL for Section 47(2) late fee + Section 50(1) interest
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
        period_match = re.search(r"Period\n(\w+)", text)
        if period_match:
            result["tax_period"] = period_match.group(1).strip()

        # 5. Financial Year
        year_match = re.search(r"Year\n(\d{4}-\d{2})", text)
        if year_match:
            result["financial_year"] = year_match.group(1).strip()

        return result

    # ── Table 3.1 — Outward supply summary ──────────────────────────────────

    def _parse_table_3_1(self, tables: List[List[List[str]]]) -> Optional[Dict[str, Any]]:
        """
        Locate Table 3.1 by scanning all extracted tables for the row identifier
        pattern '(a)', '(b)' ... '(e)' and extract taxable_value + tax amounts.

        Returns None if the table cannot be found or the format is unrecognised.
        """
        for table in tables:
            if not table or len(table) < 3:
                continue

            # Check if this table contains Table 3.1 content
            flat_text = " ".join(
                cell.lower().strip() if cell else ""
                for row in table for cell in row
            )
            if "(a)" not in flat_text or "outward taxable" not in flat_text:
                continue

            # Table 3.1 found — parse rows
            result: Dict[str, Any] = {}
            for row in table:
                if not row:
                    continue
                row_clean = [str(c).strip() if c else "" for c in row]
                row_text = " ".join(row_clean).lower()

                row_key = self._match_3_1_row(row_text)
                if row_key:
                    result[row_key] = self._extract_tax_cols_3_1(row_clean, row_key)

            if result:
                return result

        return None

    def _match_3_1_row(self, row_text: str) -> Optional[str]:
        for key, keywords in self._TABLE_3_1_ROWS.items():
            if all(kw.lower() in row_text for kw in keywords):
                return key
        return None

    def _extract_tax_cols_3_1(self, row: List[str], row_key: str) -> Dict[str, Any]:
        """
        Table 3.1 columns (current portal format):
          [0] Row label  [1] Taxable Value  [2] IGST  [3] CGST  [4] SGST/UTGST  [5] CESS
        Nil/exempted and non-GST rows only have taxable value (cols 2-5 blank).
        """
        nums = [self._parse_amount(c) for c in row]
        # Find first numeric (skip label)
        numeric_cols = [(i, v) for i, v in enumerate(nums) if v is not None]

        if not numeric_cols:
            return {}

        if row_key in ("outward_nil_exempted", "outward_non_gst"):
            return {"taxable_value": numeric_cols[0][1]}

        if len(numeric_cols) >= 5:
            return {
                "taxable_value": numeric_cols[0][1],
                "igst":          numeric_cols[1][1],
                "cgst":          numeric_cols[2][1],
                "sgst_utgst":    numeric_cols[3][1],
                "cess":          numeric_cols[4][1],
            }

        # Partial row — return what we have
        keys = ["taxable_value", "igst", "cgst", "sgst_utgst", "cess"]
        return {keys[i]: v for i, (_, v) in enumerate(numeric_cols)}

    # ── Table 6.1 — Tax payment summary ─────────────────────────────────────

    def _parse_table_6_1(self, tables: List[List[List[str]]]) -> Optional[Dict[str, Any]]:
        """
        Locate Table 6.1 by scanning for 'tax payable' / 'paid through itc' / 'paid in cash'.
        Returns None if not found.
        """
        for table in tables:
            if not table or len(table) < 2:
                continue

            flat_text = " ".join(
                cell.lower().strip() if cell else ""
                for row in table for cell in row
            )
            if "tax payable" not in flat_text and "paid in cash" not in flat_text:
                continue

            result: Dict[str, Any] = {}
            for row in table:
                if not row:
                    continue
                row_clean = [str(c).strip() if c else "" for c in row]
                row_text = " ".join(row_clean).lower()

                row_key = self._match_6_1_row(row_text)
                if row_key:
                    result[row_key] = self._extract_tax_cols_6_1(row_clean)

            if result:
                return result

        return None

    def _match_6_1_row(self, row_text: str) -> Optional[str]:
        for key, keywords in self._TABLE_6_1_ROWS.items():
            if any(kw.lower() in row_text for kw in keywords):
                return key
        return None

    def _extract_tax_cols_6_1(self, row: List[str]) -> Dict[str, Any]:
        """
        Table 6.1 columns (current portal format):
          [0] Description  [1] IGST  [2] CGST  [3] SGST/UTGST  [4] CESS
        """
        nums = [self._parse_amount(c) for c in row]
        numeric_cols = [(i, v) for i, v in enumerate(nums) if v is not None]

        if len(numeric_cols) >= 4:
            return {
                "igst":       numeric_cols[0][1],
                "cgst":       numeric_cols[1][1],
                "sgst_utgst": numeric_cols[2][1],
                "cess":       numeric_cols[3][1],
            }
        keys = ["igst", "cgst", "sgst_utgst", "cess"]
        return {keys[i]: v for i, (_, v) in enumerate(numeric_cols)}

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
