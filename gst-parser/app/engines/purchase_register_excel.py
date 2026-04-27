import io
import logging
from typing import Dict, Any, List, Optional
import pandas as pd

from app.engines.base import BaseExtractor
from app.validators.dates import normalize_date

logger = logging.getLogger(__name__)

class PurchaseRegisterExtractor(BaseExtractor):
    """
    Extracts invoice-level data from Purchase Register files.
    Supports Excel (.xlsx, .xls) and CSV formats.
    Auto-detects Tally, Busy, and GST Portal formats based on header aliases.
    """

    COLUMN_ALIASES = {
        "supplier_gstin": ["GSTIN/UIN", "Party GSTIN", "GSTIN of Supplier", "Supplier GSTIN", "GSTIN"],
        "invoice_no":     ["Vch No.", "Voucher No", "Bill No", "Invoice Number", "Inv No", "Invoice No."],
        "invoice_date":   ["Date", "Vch Date", "Bill Date", "Invoice Date", "Inv Date"],
        "taxable_value":  ["Taxable Value", "Taxable Amount", "Assessable Value", "Value"],
        "cgst":           ["CGST", "CGST Amt", "CGST Amount", "Central Tax"],
        "sgst":           ["SGST", "SGST Amt", "SGST Amount", "State Tax", "SGST/UTGST"],
        "igst":           ["IGST", "IGST Amt", "IGST Amount", "Integrated Tax"],
        "cess":           ["Cess", "Cess Amt", "Cess Amount", "GST Cess"],
        "rcm_flag":       ["Is RCM", "RCM", "Reverse Charge", "RCM Applicable", "Is Reverse Charge"],
    }

    def extract(self, content: bytes, json_data: Dict[str, Any] = None) -> Dict[str, Any]:
        result: Dict[str, Any] = {"purchase_register": []}
        
        df = self._load_dataframe(content)
        if df is None or df.empty:
            self.add_warning("Failed to parse purchase register or file is empty.")
            return result

        mapping = self._detect_columns(df)
        if not mapping.get("supplier_gstin") or not mapping.get("invoice_no"):
            self.add_warning("Could not identify essential columns (GSTIN, Invoice No) in the purchase register.")
            return result

        invoices = self._parse_rows(df, mapping)
        if not invoices:
            self.add_warning("No valid invoice rows found after parsing.")
            
        result["purchase_register"] = invoices
        return result

    def _load_dataframe(self, content: bytes) -> Optional[pd.DataFrame]:
        try:
            # Try parsing as Excel
            return pd.read_excel(io.BytesIO(content), dtype=str)
        except Exception:
            try:
                # Fallback to CSV
                # Use utf-8-sig to handle BOM which is common in Tally/Busy exports
                content_str = content.decode('utf-8-sig', errors='replace')
                return pd.read_csv(io.StringIO(content_str), dtype=str)
            except Exception as e:
                logger.error(f"Failed to load dataframe as Excel or CSV: {e}")
                return None

    def _detect_columns(self, df: pd.DataFrame) -> Dict[str, str]:
        """
        Maps standard field names to the actual DataFrame columns.
        Uses a scoring/matching approach based on predefined aliases.
        """
        mapping = {}
        # Convert df columns to lower-case for easier matching
        columns_lower = {str(col).lower().strip(): col for col in df.columns}

        for target_field, aliases in self.COLUMN_ALIASES.items():
            # Try to find a match among the aliases
            for alias in aliases:
                alias_lower = alias.lower()
                if alias_lower in columns_lower:
                    mapping[target_field] = columns_lower[alias_lower]
                    break
        return mapping

    def _parse_rows(self, df: pd.DataFrame, mapping: Dict[str, str]) -> List[Dict[str, Any]]:
        invoices = []
        for index, row in df.iterrows():
            # Skip empty rows based on essential keys
            raw_gstin = str(row.get(mapping.get("supplier_gstin", ""), "")).strip()
            raw_inv = str(row.get(mapping.get("invoice_no", ""), "")).strip()

            # Ignore empty rows or rows that clearly aren't invoices
            if not raw_gstin or raw_gstin.lower() == 'nan':
                continue
            if not raw_inv or raw_inv.lower() == 'nan':
                continue

            inv = {
                "supplier_gstin": raw_gstin,
                "invoice_no": raw_inv,
                "invoice_date": "",
                "taxable_value": 0.0,
                "igst": 0.0,
                "cgst": 0.0,
                "sgst": 0.0,
                "cess": 0.0,
                "rcm_flag": False
            }

            # Date
            if "invoice_date" in mapping:
                raw_date = str(row.get(mapping["invoice_date"], "")).strip()
                if raw_date and raw_date.lower() != 'nan':
                    norm_date = normalize_date(raw_date)
                    inv["invoice_date"] = norm_date if norm_date else raw_date

            # Amounts
            for amt_field in ["taxable_value", "igst", "cgst", "sgst", "cess"]:
                if amt_field in mapping:
                    inv[amt_field] = self._parse_amount(row.get(mapping[amt_field], ""))

            # RCM Flag
            if "rcm_flag" in mapping:
                inv["rcm_flag"] = self._parse_boolean(row.get(mapping["rcm_flag"], ""))

            invoices.append(inv)
            
        return invoices

    @staticmethod
    def _parse_amount(value: Any) -> float:
        if pd.isna(value):
            return 0.0
        val_str = str(value).replace(",", "").replace("₹", "").strip()
        if not val_str:
            return 0.0
        try:
            return float(val_str)
        except ValueError:
            return 0.0

    @staticmethod
    def _parse_boolean(value: Any) -> bool:
        if pd.isna(value):
            return False
        val_str = str(value).strip().lower()
        if val_str in ("yes", "y", "1", "true"):
            return True
        return False
