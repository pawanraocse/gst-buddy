import pytest
import io
import fitz
from app.engines.gstr9_pdf import Gstr9PdfExtractor

def create_mock_pdf_with_text(text: str) -> bytes:
    doc = fitz.open()
    page = doc.new_page()
    # Simple way to insert text (using fitz basic insertion)
    page.insert_text((50, 50), text)
    pdf_bytes = doc.write()
    doc.close()
    return pdf_bytes

@pytest.fixture
def extractor():
    return Gstr9PdfExtractor()

def test_extract_metadata(extractor):
    text = """
    FORM GSTR-9
    Annual Return
    Financial Year
    2023-24
    GSTIN
    29ABCDE1234F1Z5
    ARN
    AA291220000000A
    Date of ARN
    31/12/2024
    """
    pdf_bytes = create_mock_pdf_with_text(text)
    result = extractor.extract(pdf_bytes)
    
    assert result["gstin"] == "29ABCDE1234F1Z5"
    assert result["financial_year"] == "2023-24"
    assert result["arn"] == "AA291220000000A"
    assert result["arn_date"] == "31-12-2024"

def test_extract_table_4(extractor):
    # Regex expects: (N) Total (A to M) \n <taxable> <cgst> <sgst> <igst> <cess>
    text = """
    FORM GSTR-9
    GSTIN
    29ABCDE1234F1Z5
    (N) Total (A to M)
    1,00,000.00 9,000.00 9,000.00 0.00 0.00
    """
    pdf_bytes = create_mock_pdf_with_text(text)
    result = extractor.extract(pdf_bytes)
    
    assert "table_4" in result
    t4 = result["table_4"]
    assert t4["total_taxable_value"] == 100000.0
    assert t4["cgst"] == 9000.0
    assert t4["sgst"] == 9000.0
    assert t4["igst"] == 0.0
    assert t4["cess"] == 0.0

def test_extract_table_5(extractor):
    text = """
    FORM GSTR-9
    GSTIN
    29ABCDE1234F1Z5
    (M) Total (A to L)
    50,000.50
    """
    pdf_bytes = create_mock_pdf_with_text(text)
    result = extractor.extract(pdf_bytes)
    
    assert "table_5" in result
    assert result["table_5"]["total_exempted_value"] == 50000.5

def test_extract_empty_pdf(extractor):
    pdf_bytes = b""
    
    result = extractor.extract(pdf_bytes)
    assert not result.get("gstin")
    assert "pdfplumber error" in extractor.get_warnings()[0] or "PyMuPDF fallback also failed" in extractor.get_warnings()[0]
