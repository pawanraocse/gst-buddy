import io
import re
import logging
from typing import Dict, Any, List, Optional

import pdfplumber

from app.engines.base import BaseExtractor

logger = logging.getLogger(__name__)

class Gstr9PdfExtractor(BaseExtractor):
    """
    Extracts metadata and table data from Government GSTR-9 PDFs (Annual Return).
    
    Extracts:
    - Metadata: gstin, financial_year, arn, arn_date
    - Table 4: Details of advances, inward and outward supplies made during the financial year on which tax is payable
    - Table 5: Details of Outward supplies made during the financial year on which tax is not payable
    - Table 9: Details of tax paid as declared in returns filed during the financial year
    """

    def extract(self, content: bytes, json_data: Dict[str, Any] = None) -> Dict[str, Any]:
        result = self._extract_with_pdfplumber(content)

        if not result.get("gstin"):
            logger.warning("pdfplumber returned empty result for GSTR-9; attempting PyMuPDF fallback.")
            fallback = self._extract_with_pymupdf(content)
            if fallback:
                result = fallback

        return result

    def _extract_with_pdfplumber(self, content: bytes) -> Dict[str, Any]:
        result: Dict[str, Any] = {}
        try:
            with pdfplumber.open(io.BytesIO(content)) as pdf:
                if not pdf.pages:
                    self.add_warning("GSTR-9 PDF has no pages.")
                    return result

                # Extract text from all pages
                full_text = ""
                all_tables: List[List[List[str]]] = []
                for page in pdf.pages:
                    page_text = page.extract_text() or ""
                    full_text += "\n" + page_text
                    page_tables = page.extract_tables()
                    if page_tables:
                        all_tables.extend(page_tables)

                # 1. Metadata
                result.update(self._parse_text_metadata(full_text))

                # 2. Extract Tables
                table_4 = self._parse_table_4(full_text, all_tables)
                table_5 = self._parse_table_5(full_text, all_tables)
                table_9 = self._parse_table_9(full_text, all_tables)

                if table_4: result["table_4"] = table_4
                if table_5: result["table_5"] = table_5
                if table_9: result["table_9"] = table_9

        except Exception as e:
            self.add_warning(f"pdfplumber error for GSTR-9: {str(e)}")

        return result

    def _extract_with_pymupdf(self, content: bytes) -> Optional[Dict[str, Any]]:
        try:
            import fitz
            doc = fitz.open("pdf", content)
            if not doc:
                return None
            full_text = ""
            for page in doc:
                full_text += "\n" + page.get_text()
            doc.close()
            
            result = self._parse_text_metadata(full_text)
            
            # Simple regex fallback for tables since PyMuPDF doesn't do table extraction natively as well
            table_4 = self._parse_table_4(full_text, [])
            table_5 = self._parse_table_5(full_text, [])
            table_9 = self._parse_table_9(full_text, [])
            
            if table_4: result["table_4"] = table_4
            if table_5: result["table_5"] = table_5
            if table_9: result["table_9"] = table_9
                
            return result
        except Exception as e:
            self.add_warning(f"PyMuPDF fallback also failed for GSTR-9: {str(e)}")
            return None

    def _parse_text_metadata(self, text: str) -> Dict[str, Any]:
        result: Dict[str, Any] = {}

        gstin_match = re.search(r"GSTIN[^\n]*\n([0-9A-Z]{15})", text)
        if not gstin_match:
            gstin_match = re.search(r"\b([0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[0-9A-Z]{1}[Z]{1}[0-9A-Z]{1})\b", text)
        if gstin_match:
            result["gstin"] = gstin_match.group(1)

        year_match = re.search(r"Financial year\n(\d{4}-\d{2})", text, re.IGNORECASE)
        if not year_match:
            year_match = re.search(r"Financial Year[^\n]*\n(\d{4}-\d{2})", text, re.IGNORECASE)
        if year_match:
            result["financial_year"] = year_match.group(1).strip()

        arn_match = re.search(r"ARN[^\n]*\n([A-Z0-9]+)", text)
        if arn_match:
            result["arn"] = arn_match.group(1)

        arn_date_match = re.search(r"(?:ARN date|Date of ARN|Date of filing)[^\n]*\n(\d{2}[/\-]\d{2}[/\-]\d{4})", text, re.IGNORECASE)
        if arn_date_match:
            result["arn_date"] = arn_date_match.group(1).replace("/", "-")

        return result

    def _parse_table_4(self, full_text: str, all_tables: List[List[List[str]]]) -> Dict[str, Any]:
        """Table 4: Details of advances, inward and outward supplies on which tax is payable"""
        result = {
            "total_taxable_value": 0.0,
            "igst": 0.0,
            "cgst": 0.0,
            "sgst": 0.0,
            "cess": 0.0
        }
        
        # Regex extraction strategy for the total row (usually "4N" or "Total")
        # GSTR-9 Table 4 total row format: (N) Total (A to M)
        match = re.search(r"\(N\)\s*Total(?:.*?)\n([\d,]+\.?\d*)\s+([\d,]+\.?\d*)\s+([\d,]+\.?\d*)\s+([\d,]+\.?\d*)\s+([\d,]+\.?\d*)", full_text, re.IGNORECASE)
        if match:
            result["total_taxable_value"] = self._parse_amount(match.group(1)) or 0.0
            result["cgst"] = self._parse_amount(match.group(2)) or 0.0
            result["sgst"] = self._parse_amount(match.group(3)) or 0.0
            result["igst"] = self._parse_amount(match.group(4)) or 0.0
            result["cess"] = self._parse_amount(match.group(5)) or 0.0
            return result
            
        # Fallback to tables
        for table in all_tables:
            for row in table:
                if not row: continue
                row_text = " ".join(str(c).lower().strip() if c else "" for c in row)
                if "(n)" in row_text and "total" in row_text:
                    nums = [self._parse_amount(str(c)) for c in row if self._parse_amount(str(c)) is not None]
                    if len(nums) >= 5:
                        result["total_taxable_value"] = nums[0]
                        result["cgst"] = nums[1]
                        result["sgst"] = nums[2]
                        result["igst"] = nums[3]
                        result["cess"] = nums[4]
                        return result
        return result

    def _parse_table_5(self, full_text: str, all_tables: List[List[List[str]]]) -> Dict[str, Any]:
        """Table 5: Details of Outward supplies on which tax is not payable"""
        result = {
            "total_exempted_value": 0.0
        }
        # Total usually at (M) Total or (N) Total Turnover (4N + 5M - 4G)
        match = re.search(r"\(M\)\s*Total(?:.*?)\n([\d,]+\.?\d*)", full_text, re.IGNORECASE)
        if match:
            result["total_exempted_value"] = self._parse_amount(match.group(1)) or 0.0
            return result
            
        for table in all_tables:
            for row in table:
                if not row: continue
                row_text = " ".join(str(c).lower().strip() if c else "" for c in row)
                if "(m)" in row_text and "total" in row_text:
                    nums = [self._parse_amount(str(c)) for c in row if self._parse_amount(str(c)) is not None]
                    if len(nums) >= 1:
                        result["total_exempted_value"] = nums[0]
                        return result
        return result

    def _parse_table_9(self, full_text: str, all_tables: List[List[List[str]]]) -> Dict[str, Any]:
        """Table 9: Details of tax paid as declared in returns filed during the financial year"""
        result = {
            "tax_payable": {"igst": 0.0, "cgst": 0.0, "sgst": 0.0, "cess": 0.0},
            "tax_paid_cash": {"igst": 0.0, "cgst": 0.0, "sgst": 0.0, "cess": 0.0},
            "tax_paid_itc": {"igst": 0.0, "cgst": 0.0, "sgst": 0.0, "cess": 0.0}
        }
        
        # Table 9 rows: IGST, CGST, SGST, Cess
        for table in all_tables:
            for row in table:
                if not row: continue
                row_text = " ".join(str(c).lower().strip() if c else "" for c in row)
                
                # Columns: Description, Tax Payable, Paid through cash, Paid through ITC (IGST, CGST, SGST, CESS)
                # We expect rows for Integrated Tax, Central Tax, State/UT Tax, Cess
                
                nums = [self._parse_amount(str(c)) for c in row if self._parse_amount(str(c)) is not None]
                if len(nums) >= 6:
                    tax_type = "unknown"
                    if "integrated" in row_text: tax_type = "igst"
                    elif "central" in row_text: tax_type = "cgst"
                    elif "state" in row_text or "ut" in row_text: tax_type = "sgst"
                    elif "cess" in row_text: tax_type = "cess"
                    
                    if tax_type != "unknown":
                        result["tax_payable"][tax_type] = nums[0]
                        result["tax_paid_cash"][tax_type] = nums[1]
                        # The rest are Paid through ITC (IGST, CGST, SGST/UTGST, CESS)
                        # Summing them up as total paid through ITC for this tax type
                        result["tax_paid_itc"][tax_type] = sum(nums[2:6])

        return result

    @staticmethod
    def _parse_amount(value: str) -> Optional[float]:
        if not value: return None
        cleaned = value.replace(",", "").replace("₹", "").strip()
        if not cleaned: return None
        try: return float(cleaned)
        except ValueError: return None
