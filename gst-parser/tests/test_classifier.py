import json
from app.classifier.classifier import classify_document
from app.api.models import ClassifierConfidence

def test_classify_gstr1_json():
    dummy_json = {
        "gstin": "07ASXPD9282E1Z8",
        "fp": "042024",
        "filing_typ": "M",
        "fil_dt": "11-05-2024"
    }
    content = json.dumps(dummy_json).encode("utf-8")
    
    doc_type, confidence, parsed_data = classify_document(content, "returns_10012026.json")
    
    assert doc_type == "GSTR1_JSON"
    assert confidence == ClassifierConfidence.HIGH
    assert parsed_data is not None
    assert parsed_data["gstin"] == "07ASXPD9282E1Z8"

def test_classify_gstr2a_json():
    # Only gstin and fp are guaranteed core keys for GSTR-2A, plus some data nodes
    dummy_json = {
        "gstin": "07ASXPD9282E1Z8",
        "fp": "042024",
        "b2b": []
    }
    content = json.dumps(dummy_json).encode("utf-8")
    
    doc_type, confidence, parsed_data = classify_document(content, "returns_10012026.json")
    
    assert doc_type == "GSTR2A_JSON"
    assert confidence == ClassifierConfidence.HIGH

def test_classify_unknown():
    content = b"random garbage binary content not matching any PDF magic or JSON"
    doc_type, confidence, parsed_data = classify_document(content, "garbage.bin")
    
    assert doc_type == "UNKNOWN"
    assert confidence == ClassifierConfidence.UNKNOWN

def test_classify_fallback_hint():
    content = b"random garbage binary content not matching any PDF magic or JSON"
    doc_type, confidence, parsed_data = classify_document(content, "garbage.bin", hint="GSTR3B_PDF")
    
    assert doc_type == "GSTR3B_PDF"
    assert confidence == ClassifierConfidence.LOW
