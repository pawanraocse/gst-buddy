import os
from pathlib import Path
import json

from app.engines.gstr1_pdf import Gstr1PdfExtractor
from app.engines.gstr3b_pdf import Gstr3bPdfExtractor
from app.engines.json_engine import JsonEngine

# Derive the absolute path to the resources directory from the project root
PROJECT_ROOT = Path(__file__).parent.parent.parent
RESOURCES_DIR = PROJECT_ROOT / "resources" / "PROJECT GST" / "RAW RETURN"

def test_extract_gstr1_pdf_real_document():
    pdf_path = RESOURCES_DIR / "GSTR1_07ASXPD9282E1Z8_042024.pdf"
    
    # We only run assertion if the file actually exists to not fail in pure CI
    if not pdf_path.exists():
        return
        
    with open(pdf_path, 'rb') as f:
        content = f.read()

    extractor = Gstr1PdfExtractor()
    result = extractor.extract(content)
    
    # These assertions test the specific patterns used by the PyMuPDF scraper on the official PDF format
    assert result.get("gstin") == "07ASXPD9282E1Z8"
    assert result.get("tax_period") in ["April", "Apr", "042024"]
    assert result.get("financial_year") in ["2024-25", "2023-24"]
    # We should have ARN and ARN date if the document is filed
    assert "arn" in result
    assert "arn_date" in result


def test_extract_gstr3b_pdf_real_document():
    pdf_path = RESOURCES_DIR / "GSTR3B_07ASXPD9282E1Z8_042024.pdf"
    
    if not pdf_path.exists():
        return
        
    with open(pdf_path, 'rb') as f:
        content = f.read()

    extractor = Gstr3bPdfExtractor()
    result = extractor.extract(content)
    
    # Verify the fallback logic correctly parses the GSTR-3B PDF anchors
    assert result.get("gstin") == "07ASXPD9282E1Z8"
    assert "arn" in result
    assert "arn_date" in result


def test_extract_gstr1_json_real_document():
    # Read one of the JSON files from the offline download folder
    json_dir = RESOURCES_DIR / "returns_10012026_R1_07ASXPD9282E1Z8_offline_others_0"
    if not json_dir.exists():
        return
        
    # Example json file in the folder (we test against b2b.json or similar)
    json_path = list(json_dir.glob("*.json"))[0] if list(json_dir.glob("*.json")) else None
    if not json_path:
        return
        
    with open(json_path, 'rb') as f:
        content = f.read()

    extractor = JsonEngine()
    result = extractor.extract(content)
    
    assert "gstin" in result
    assert "fp" in result


def test_extract_gstr2a_json_real_document():
    json_dir = RESOURCES_DIR / "07ASXPD9282E1Z8_042024_R2A"
    if not json_dir.exists():
        return
        
    json_path = list(json_dir.glob("*.json"))[0] if list(json_dir.glob("*.json")) else None
    if not json_path:
        return
        
    with open(json_path, 'rb') as f:
        content = f.read()

    extractor = JsonEngine()
    result = extractor.extract(content)
    
    assert "gstin" in result
    assert "fp" in result

