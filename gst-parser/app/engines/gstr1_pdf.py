import re
import fitz # PyMuPDF
from typing import Dict, Any

from app.engines.base import BaseExtractor
from app.validators.dates import normalize_date

class Gstr1PdfExtractor(BaseExtractor):
    """
    Extracts text anchored by known headers in Government GSTR-1 PDFs.
    """
    def extract(self, content: bytes, json_data: Dict[str, Any] = None) -> Dict[str, Any]:
        result = {}
        
        try:
            doc = fitz.open("pdf", content)
            
            # We only need the first page for the main metrics (ARN, Date, GSTIN)
            if len(doc) > 0:
                text = doc[0].get_text()
                
                # 1. GSTIN (Any text followed by newline and 15 chars)
                gstin_match = re.search(r"GSTIN[^\n]*\n([0-9A-Z]{15})", text)
                if not gstin_match:
                    gstin_match = re.search(r"([0-9A-Z]{15})", text)
                if gstin_match:
                    result["gstin"] = gstin_match.group(1)
                
                # 2. ARN
                arn_match = re.search(r"ARN[^\n]*\n([A-Z0-9]+)", text)
                if arn_match:
                    result["arn"] = arn_match.group(1)
                    
                # 3. ARN Date (CRITICAL for late fees)
                arn_date_match = re.search(r"(?:ARN date|Date of ARN|Date of filing)[^\n]*\n(\d{2}/\d{2}/\d{4})", text, re.IGNORECASE)
                if arn_date_match:
                    normalized = normalize_date(arn_date_match.group(1))
                    result["arn_date"] = normalized
                    if not normalized:
                        self.add_warning(f"Failed to normalize ARN Date: {arn_date_match.group(1)}")
                
                # 4. Tax Period & Year
                period_match = re.search(r"Tax period\n(\w+)", text)
                if period_match:
                    result["tax_period"] = period_match.group(1).strip()
                    
                year_match = re.search(r"Financial year\n(\d{4}-\d{2})", text)
                if year_match:
                    result["financial_year"] = year_match.group(1).strip()
        
        except Exception as e:
            self.add_warning(f"Error extracting GSTR-1 PDF: {str(e)}")
            
        return result
