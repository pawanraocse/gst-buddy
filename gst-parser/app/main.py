import time
from fastapi import FastAPI, UploadFile, File, Form, HTTPException
from fastapi.responses import JSONResponse
import logging

from app.api.models import ParsedDocumentResponse, ParserErrorModel, ExtractionStatus, ClassifierConfidence
from app.classifier.classifier import classify_document
from app.engines.json_engine import JsonEngine
from app.engines.gstr1_pdf import Gstr1PdfExtractor
from app.engines.gstr3b_pdf import Gstr3bPdfExtractor
from app.engines.gstr2a_pdf import Gstr2aPdfExtractor
from app.engines.gstr2b_pdf import Gstr2bPdfExtractor
from app.engines.gstr9_pdf import Gstr9PdfExtractor

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="GST Document Parser", version="1.0.0")

@app.get("/health")
async def health_check():
    return {"status": "up", "version": app.version}

@app.post("/api/v1/extract", response_model=ParsedDocumentResponse)
async def extract_document(
    file: UploadFile = File(...),
    doc_type_hint: str = Form(None)
):
    start_time = time.time()
    logger.info(f"Received file for extraction: {file.filename}, hint: {doc_type_hint}")

    content = await file.read()
    if not content:
        return JSONResponse(
            status_code=422,
            content=ParserErrorModel(
                error_code="EMPTY_FILE",
                error_message="The uploaded file is empty.",
                suggestions=["Upload a valid PDF or JSON downloaded from the GST portal."]
            ).model_dump()
        )

    # Stage 1: Classify
    doc_type, confidence, parsed_json = classify_document(content, file.filename, doc_type_hint)

    # Stage 2 & 3: Route and Extract
    extracted_data = {}
    warnings = []
    
    try:
        if doc_type == "GSTR1_JSON" or doc_type == "GSTR2A_JSON":
            engine = JsonEngine()
            extracted_data = engine.extract(content, parsed_json)
        elif doc_type == "GSTR1_PDF":
            engine = Gstr1PdfExtractor()
            extracted_data = engine.extract(content)
        elif doc_type == "GSTR3B_PDF":
            engine = Gstr3bPdfExtractor()
            extracted_data = engine.extract(content)
        elif doc_type == "GSTR2A_PDF":
            engine = Gstr2aPdfExtractor()
            extracted_data = engine.extract(content)
        elif doc_type == "GSTR2B_PDF":
            engine = Gstr2bPdfExtractor()
            extracted_data = engine.extract(content)
        elif doc_type == "GSTR9_PDF":
            engine = Gstr9PdfExtractor()
            extracted_data = engine.extract(content)
        elif doc_type == "PURCHASE_REGISTER":
            from app.engines.purchase_register_excel import PurchaseRegisterExtractor
            engine = PurchaseRegisterExtractor()
            extracted_data = engine.extract(content)
        else:
            return JSONResponse(
                status_code=422,
                content=ParserErrorModel(
                    error_code="UNKNOWN_FORMAT",
                    error_message=f"Could not process document. Detected type: {doc_type}.",
                    suggestions=["Check file structure matches GST portal downloaded files"]
                ).model_dump()
            )
            
        warnings.extend(engine.get_warnings())
    except Exception as e:
        logger.error(f"Extraction failed for {doc_type}: {e}")
        return JSONResponse(
            status_code=500,
            content=ParserErrorModel(
                error_code="EXTRACTION_ERROR",
                error_message=f"Failed to extract document: {str(e)}"
            ).model_dump()
        )

    # Stage 4: Validate
    # Minimal validation here, deeper checks happen on Java side
    if "arn_date" not in extracted_data and doc_type in ("GSTR1_PDF", "GSTR3B_PDF"):
        warnings.append("ARN Date missing. Late fee calculation may fail downstream.")

    duration_ms = int((time.time() - start_time) * 1000)
    
    return ParsedDocumentResponse(
        status=ExtractionStatus.SUCCESS,
        doc_type=doc_type,
        classifier_confidence=confidence,
        extracted_data=extracted_data,
        extraction_time_ms=duration_ms,
        parser_version=app.version,
        warnings=warnings
    )
