import io
import pandas as pd
import pytest

from app.engines.purchase_register_excel import PurchaseRegisterExtractor

@pytest.fixture
def extractor():
    return PurchaseRegisterExtractor()

def test_tally_format_excel(extractor):
    # Create an in-memory excel file matching Tally format
    df = pd.DataFrame({
        "Particulars": ["Supplier A", "Supplier B"],
        "Vch No.": ["INV-001", "INV-002"],
        "Vch Date": ["01/04/2024", "05-04-2024"],
        "GSTIN/UIN": ["29ABCDE1234F1Z5", "27ABCDE1234F1Z5"],
        "Taxable Value": [1000.0, 2000.0],
        "CGST": [90.0, 0.0],
        "SGST": [90.0, 0.0],
        "IGST": [0.0, 360.0],
        "Cess": [0.0, 0.0],
        "Is RCM": ["Yes", "No"]
    })
    
    excel_io = io.BytesIO()
    df.to_excel(excel_io, index=False)
    excel_content = excel_io.getvalue()
    
    result = extractor.extract(excel_content)
    assert not extractor.get_warnings()
    assert "purchase_register" in result
    
    invoices = result["purchase_register"]
    assert len(invoices) == 2
    
    inv1 = invoices[0]
    assert inv1["supplier_gstin"] == "29ABCDE1234F1Z5"
    assert inv1["invoice_no"] == "INV-001"
    assert inv1["invoice_date"] == "2024-04-01"
    assert inv1["taxable_value"] == 1000.0
    assert inv1["cgst"] == 90.0
    assert inv1["igst"] == 0.0
    assert inv1["rcm_flag"] is True

def test_busy_format_csv(extractor):
    # Busy format uses different headers and "1"/"0" for RCM
    df = pd.DataFrame({
        "Party Name": ["Supplier X"],
        "Bill No": ["B-100"],
        "Bill Date": ["2024-05-15"],
        "Party GSTIN": ["07XXXXX0000X1Z5"],
        "Assessable Value": ["5,000.00"],
        "Central Tax": ["450.00"],
        "State Tax": ["450.00"],
        "Integrated Tax": ["0"],
        "RCM": ["1"]
    })
    
    csv_io = io.StringIO()
    df.to_csv(csv_io, index=False)
    # the extractor expects bytes
    csv_content = csv_io.getvalue().encode('utf-8')
    
    result = extractor.extract(csv_content)
    assert not extractor.get_warnings()
    
    inv = result["purchase_register"][0]
    assert inv["supplier_gstin"] == "07XXXXX0000X1Z5"
    assert inv["invoice_no"] == "B-100"
    assert inv["invoice_date"] == "2024-05-15"
    assert inv["taxable_value"] == 5000.0
    assert inv["cgst"] == 450.0
    assert inv["rcm_flag"] is True

def test_portal_format_csv_with_bom(extractor):
    # GST portal export
    df = pd.DataFrame({
        "GSTIN of Supplier": ["33ABCDE1234F1Z5"],
        "Invoice Number": ["12345"],
        "Invoice Date": ["31/12/2023"],
        "Value": ["₹ 1,500.50"],
        "IGST Amt": ["270.09"],
        "Reverse Charge": ["Y"]
    })
    
    csv_io = io.StringIO()
    df.to_csv(csv_io, index=False)
    csv_content = csv_io.getvalue().encode('utf-8-sig') # Add BOM
    
    result = extractor.extract(csv_content)
    assert not extractor.get_warnings()
    
    inv = result["purchase_register"][0]
    assert inv["supplier_gstin"] == "33ABCDE1234F1Z5"
    assert inv["invoice_no"] == "12345"
    assert inv["taxable_value"] == 1500.50
    assert inv["igst"] == 270.09
    assert inv["cgst"] == 0.0
    assert inv["rcm_flag"] is True

def test_empty_or_invalid_file(extractor):
    # Test completely garbage bytes
    result = extractor.extract(b"Not a valid excel or csv file format \x00\xff")
    assert "Failed to parse purchase register" in extractor.get_warnings()[0]
    
    # Test valid excel but missing essential columns
    df = pd.DataFrame({"Some Col": [1, 2], "Another Col": ["A", "B"]})
    excel_io = io.BytesIO()
    df.to_excel(excel_io, index=False)
    result = extractor.extract(excel_io.getvalue())
    assert "Could not identify essential columns" in extractor.get_warnings()[-1]

def test_ignore_empty_rows(extractor):
    df = pd.DataFrame({
        "GSTIN": ["29ABCDE1234F1Z5", "", "30ABCDE1234F1Z5", None],
        "Invoice No.": ["INV-1", "INV-2", None, "INV-4"]
    })
    excel_io = io.BytesIO()
    df.to_excel(excel_io, index=False)
    
    result = extractor.extract(excel_io.getvalue())
    invoices = result["purchase_register"]
    # Only the first row has both GSTIN and Invoice No
    assert len(invoices) == 1
    assert invoices[0]["supplier_gstin"] == "29ABCDE1234F1Z5"
