import io
import re
import logging
from typing import Dict, Any, List, Optional

import pdfplumber

from app.engines.base import BaseExtractor
from app.validators.dates import normalize_date

logger = logging.getLogger(__name__)

class Gstr2bPdfExtractor(BaseExtractor):
    """
    Extracts metadata and ITC rows from Government GSTR-2B PDFs.
    """
    def extract(self, content: bytes, json_data: Dict[str, Any] = None) -> Dict[str, Any]:
        result = self._extract_with_pdfplumber(content)
        if not result.get("gstin"):
            logger.warning("pdfplumber returned empty result for GSTR-2B; attempting PyMuPDF fallback.")
            fallback = self._extract_with_pymupdf(content)
            if fallback:
                result = fallback
        return result

    def _extract_with_pdfplumber(self, content: bytes) -> Dict[str, Any]:
        result: Dict[str, Any] = {}
        try:
            with pdfplumber.open(io.BytesIO(content)) as pdf:
                if not pdf.pages:
                    self.add_warning("GSTR-2B PDF has no pages.")
                    return result
                
                text = pdf.pages[0].extract_text() or ""
                result = self._parse_text(text)
                
                result["itc_rows"] = self._parse_tables(pdf.pages)
        except Exception as e:
            self.add_warning(f"pdfplumber error for GSTR-2B: {str(e)}")
        return result

    def _extract_with_pymupdf(self, content: bytes) -> Optional[Dict[str, Any]]:
        try:
            import fitz
            doc = fitz.open("pdf", content)
            if not doc: return None
            text = doc[0].get_text()
            doc.close()
            return self._parse_text(text)
        except Exception as e:
            self.add_warning(f"PyMuPDF fallback failed for GSTR-2B: {str(e)}")
            return None

    def _parse_text(self, text: str) -> Dict[str, Any]:
        result: Dict[str, Any] = {}
        
        gstin_match = re.search(r"GSTIN[^\n]*\n([0-9A-Z]{15})", text, re.IGNORECASE)
        if not gstin_match:
            gstin_match = re.search(r"\b([0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[0-9A-Z]{1}[Z]{1}[0-9A-Z]{1})\b", text)
        if gstin_match: result["gstin"] = gstin_match.group(1)

        period_match = re.search(r"Period\n(\w+)", text)
        if period_match: result["tax_period"] = period_match.group(1).strip()

        year_match = re.search(r"Year\n(\d{4}-\d{2})", text)
        if year_match: result["financial_year"] = year_match.group(1).strip()
        
        return result

    def _parse_tables(self, pages: List[Any]) -> List[Dict[str, Any]]:
        itc_rows = []
        for page in pages:
            tables = page.extract_tables()
            for table in tables:
                for row in table:
                    if not row or len(row) < 5: continue
                    row_clean = [str(c).strip() if c else "" for c in row]
                    
                    if re.match(r"^[0-9A-Z]{15}$", row_clean[0]):
                        try:
                            itc_rows.append({
                                "supplier_gstin": row_clean[0],
                                "invoice_no": row_clean[1] if len(row_clean) > 1 else "",
                                "invoice_date": normalize_date(row_clean[2]) if len(row_clean) > 2 else "",
                                "taxable_value": self._parse_amount(row_clean[3]) if len(row_clean) > 3 else 0.0,
                                "igst": self._parse_amount(row_clean[4]) if len(row_clean) > 4 else 0.0,
                                "cgst": self._parse_amount(row_clean[5]) if len(row_clean) > 5 else 0.0,
                                "sgst": self._parse_amount(row_clean[6]) if len(row_clean) > 6 else 0.0,
                                "cess": self._parse_amount(row_clean[7]) if len(row_clean) > 7 else 0.0,
                                "itc_available": True
                            })
                        except Exception:
                            pass
        return itc_rows

    @staticmethod
    def _parse_amount(value: str) -> float:
        if not value: return 0.0
        cleaned = value.replace(",", "").replace("₹", "").strip()
        try: return float(cleaned)
        except ValueError: return 0.0
