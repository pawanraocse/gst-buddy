from enum import Enum
from typing import Dict, Any, List, Optional
from pydantic import BaseModel

class ExtractionStatus(str, Enum):
    SUCCESS = "SUCCESS"
    FAILED = "FAILED"

class ClassifierConfidence(str, Enum):
    HIGH = "HIGH"
    LOW = "LOW"
    UNKNOWN = "UNKNOWN"

class ParsedDocumentResponse(BaseModel):
    status: ExtractionStatus
    doc_type: str
    classifier_confidence: ClassifierConfidence
    extracted_data: Optional[Dict[str, Any]] = None
    extraction_time_ms: int
    parser_version: str
    warnings: List[str] = []

class ParserErrorModel(BaseModel):
    status: ExtractionStatus = ExtractionStatus.FAILED
    error_code: str
    error_message: str
    suggestions: List[str] = []
