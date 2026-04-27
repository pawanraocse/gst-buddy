import json
import logging
from typing import Optional, Dict, Any, Tuple
import fitz  # PyMuPDF

from app.classifier.fingerprints import PDF_FINGERPRINTS, JSON_KEYS_FINGERPRINTS
from app.api.models import ClassifierConfidence

logger = logging.getLogger(__name__)

def attempt_pdf_classification(content: bytes) -> Optional[str]:
    """Reads first page of a PDF in-memory to classify against known fingerprints."""
    try:
        # Open PDF from memory
        doc = fitz.open("pdf", content)
        if len(doc) == 0:
            return None
            
        first_page_text = doc[0].get_text()
        
        for fp in PDF_FINGERPRINTS:
            # Check if ALL required keywords are in the text
            if all(kw in first_page_text for kw in fp.required_keywords):
                logger.info(f"Classified PDF as {fp.doc_type}")
                return fp.doc_type
                
        doc.close()
    except Exception as e:
        logger.error(f"Error during PDF classification: {e}")
        
    return None

def attempt_json_classification(content: bytes) -> Tuple[Optional[str], Optional[Dict[str, Any]]]:
    """Parses JSON and checks keys against known patterns."""
    try:
        data = json.loads(content)
        if not isinstance(data, dict):
            return None, data
            
        keys = set(data.keys())
        
        # Check from most specific to least specific
        if JSON_KEYS_FINGERPRINTS["GSTR1_JSON"].issubset(keys):
            return "GSTR1_JSON", data
            
        # Needs to be purely JSON, so if it has typical 2A offline json keys
        if JSON_KEYS_FINGERPRINTS["GSTR2A_JSON"].issubset(keys) and "data" in keys or "b2b" in keys:
            return "GSTR2A_JSON", data
            
    except Exception as e:
        # Not a valid JSON file
        pass
        
    return None, None

def attempt_excel_csv_classification(filename: str) -> Optional[str]:
    """Classifies based on file extension for Excel/CSV files."""
    if not filename:
        return None
    lower_name = filename.lower()
    if lower_name.endswith('.xlsx') or lower_name.endswith('.xls') or lower_name.endswith('.csv'):
        return "PURCHASE_REGISTER"
    return None

def classify_document(content: bytes, filename: str, hint: Optional[str] = None) -> Tuple[str, ClassifierConfidence, Optional[Dict[str, Any]]]:
    """
    Evaluates the binary content to classify its document type.
    """
    # 1. Try to classify as JSON first (quick failure if it's a PDF)
    doc_type, json_data = attempt_json_classification(content)
    if doc_type:
        return doc_type, ClassifierConfidence.HIGH, json_data
        
    # 2. Try to classify as PDF
    doc_type = attempt_pdf_classification(content)
    if doc_type:
        return doc_type, ClassifierConfidence.HIGH, None
        
    # 3. Try Excel/CSV
    doc_type = attempt_excel_csv_classification(filename)
    if doc_type:
        return doc_type, ClassifierConfidence.MEDIUM, None
        
    # 4. Fallback to Hint
    if hint:
        logger.warning(f"Classification failed. Falling back to hint: {hint}")
        return hint, ClassifierConfidence.LOW, None
        
    return "UNKNOWN", ClassifierConfidence.UNKNOWN, None
