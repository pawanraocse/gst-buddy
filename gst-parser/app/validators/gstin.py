import re

def is_valid_gstin(gstin: str) -> bool:
    """
    Validates the structure of an Indian GSTIN.
    Format: 2 digits (state code) + 10 Alphanumeric (PAN) + 
            1 Alphanumeric (Entity num) + 'Z' (Default) + 1 Alphanumeric (Checksum)
    """
    if not gstin or len(gstin) != 15:
        return False
        
    pattern = r"^[0-3][0-9][A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$"
    return bool(re.match(pattern, gstin.upper()))

def get_state_code(gstin: str) -> str:
    """Extracts the state code from a valid GSTIN."""
    if not gstin or len(gstin) < 2:
        return ""
    return gstin[:2]
