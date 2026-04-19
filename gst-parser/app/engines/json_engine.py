from typing import Dict, Any
from app.engines.base import BaseExtractor
from app.validators.dates import normalize_date

class JsonEngine(BaseExtractor):
    """
    Passthrough extractor for portal-downloaded JSON files.
    Validates and normalizes dates and formatting.
    """
    def extract(self, content: bytes, json_data: Dict[str, Any] = None) -> Dict[str, Any]:
        if not json_data:
            raise ValueError("JSON Engine requires pre-parsed json_data dictionary.")
            
        result = {}
        
        # Standardize basic headers for GSTR-1 JSON
        if "gstin" in json_data:
            result["gstin"] = json_data["gstin"]
            
        # fp is formatted like "042024"
        if "fp" in json_data:
            result["tax_period_code"] = json_data["fp"]
            
        if "fil_dt" in json_data:
            normalized = normalize_date(json_data["fil_dt"])
            result["arn_date"] = normalized
            if not normalized:
                self.add_warning(f"Could not normalize fil_dt: {json_data['fil_dt']}")
                
        if "filing_typ" in json_data:
            # M = Monthly, Q = Quarterly
            result["filing_status"] = json_data["filing_typ"]
            
        # The remainder of the JSON represents the tables, 
        # which can be passed straight through for Phase 2 reconciliation.
        result["tables"] = {k: v for k, v in json_data.items() if k not in ['gstin', 'fp', 'fil_dt', 'filing_typ']}
        
        return result
