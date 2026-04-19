"""
Registry of Document Types and their classification fingerprints.
"""
from datac.lasses import dataclass
from typing import List

@dataclass
class DocumentFingerprint:
    doc_type: str
    required_keywords: List[str]

# For PDFs, we scan the first page for these text keywords
PDF_FINGERPRINTS = [
    DocumentFingerprint(
        doc_type="GSTR1_PDF",
        required_keywords=["FORM GSTR-1", "[See rule 59(1)]"]
    ),
    DocumentFingerprint(
        doc_type="GSTR3B_PDF",
        required_keywords=["Form GSTR-3B", "[See rule 61(5)]"]
    )
]

# For JSON files, we rely on the presence of specific keys
JSON_KEYS_FINGERPRINTS = {
    "GSTR1_JSON": {"gstin", "fp", "filing_typ", "fil_dt"},
    "GSTR2A_JSON": {"gstin", "fp"}  # Usually true for offline JSON from portal
}
