from abc import ABC, abstractmethod
from typing import Dict, Any, List

class BaseExtractor(ABC):
    """
    Abstract base class for all document extractors.
    """
    def __init__(self):
        self.warnings: List[str] = []
        
    @abstractmethod
    def extract(self, content: bytes, json_data: Dict[str, Any] = None) -> Dict[str, Any]:
        """
        Executes the extraction logic.
        :param content: Binary file content
        :param json_data: Pre-parsed JSON dict if the file was JSON
        :return: Standardized dictionary of extracted data
        """
        pass

    def add_warning(self, message: str):
        self.warnings.append(message)
        
    def get_warnings(self) -> List[str]:
        return self.warnings
