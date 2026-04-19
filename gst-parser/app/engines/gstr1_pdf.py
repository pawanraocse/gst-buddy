import io
import re
import logging
from typing import Dict, Any, Optional

import pdfplumber

from app.engines.base import BaseExtractor
from app.validators.dates import normalize_date

logger = logging.getLogger(__name__)


class Gstr1PdfExtractor(BaseExtractor):
    """
    Extracts metadata from Government GSTR-1 PDFs.

    Default backend: pdfplumber (precise character-level coordinate extraction).
    Fallback:        PyMuPDF (fitz) — used only when pdfplumber fails to open the PDF.

    Extracted fields (all from first page):
      - gstin        : 15-char taxpayer GSTIN
      - arn          : Acknowledgement Reference Number
      - arn_date     : Filing date (ISO 8601, CRITICAL for late fee computation)
      - tax_period   : e.g. "March" / "April"
      - financial_year: e.g. "2024-25"
    """

    def extract(self, content: bytes, json_data: Dict[str, Any] = None) -> Dict[str, Any]:
        result = self._extract_with_pdfplumber(content)

        # If pdfplumber produced an empty result (no GSTIN, no arn_date), try fallback
        if not result.get("gstin") and not result.get("arn_date"):
            logger.warning("pdfplumber returned empty result for GSTR-1; attempting PyMuPDF fallback.")
            fallback = self._extract_with_pymupdf(content)
            if fallback:
                result = fallback

        return result

    # ──────────────────────────────────────────────────────────────────────────
    # Primary: pdfplumber
    # ──────────────────────────────────────────────────────────────────────────

    def _extract_with_pdfplumber(self, content: bytes) -> Dict[str, Any]:
        result: Dict[str, Any] = {}
        try:
            with pdfplumber.open(io.BytesIO(content)) as pdf:
                if not pdf.pages:
                    self.add_warning("GSTR-1 PDF has no pages.")
                    return result

                text = pdf.pages[0].extract_text() or ""
                result = self._parse_text(text)

        except Exception as e:
            self.add_warning(f"pdfplumber error for GSTR-1: {str(e)}")

        return result

    # ──────────────────────────────────────────────────────────────────────────
    # Fallback: PyMuPDF
    # ──────────────────────────────────────────────────────────────────────────

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
            self.add_warning(f"PyMuPDF fallback also failed for GSTR-1: {str(e)}")
            return None

    # ──────────────────────────────────────────────────────────────────────────
    # Shared parsing logic — works on raw text regardless of backend
    # ──────────────────────────────────────────────────────────────────────────

    def _parse_text(self, text: str) -> Dict[str, Any]:
        result: Dict[str, Any] = {}

        # 1. GSTIN — 15 alphanumeric chars after the GSTIN label (or any 15-char match)
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
