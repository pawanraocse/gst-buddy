import re
from datetime import datetime
from typing import Optional

def normalize_date(date_str: str) -> Optional[str]:
    """
    Normalizes various date formats found in GST documents to ISO 8601 (YYYY-MM-DD).
    Handles:
    - 11/05/2024 -> 2024-05-11
    - 11-05-2024 -> 2024-05-11
    """
    if not date_str:
        return None
        
    date_str = date_str.strip()
    
    # Define potential GST date formats
    formats = [
        "%d/%m/%Y",
        "%d-%m-%Y",
        "%Y-%m-%d"
    ]
    
    for fmt in formats:
        try:
            dt = datetime.strptime(date_str, fmt)
            return dt.strftime("%Y-%m-%d")
        except ValueError:
            continue
            
    return None
