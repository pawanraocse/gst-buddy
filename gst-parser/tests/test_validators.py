import pytest
from app.validators.gstin import is_valid_gstin, get_state_code
from app.validators.dates import normalize_date

def test_valid_gstin():
    assert is_valid_gstin("07ASXPD9282E1Z8") == True
    assert is_valid_gstin("29ABCDE1234F2Z5") == True

def test_invalid_gstin():
    assert is_valid_gstin("INVALIDGSTIN") == False
    assert is_valid_gstin("") == False
    assert is_valid_gstin(None) == False
    assert is_valid_gstin("07ASXPD9282E1Z8EXTRA") == False

def test_get_state_code():
    assert get_state_code("07ASXPD9282E1Z8") == "07"
    assert get_state_code("") == ""

def test_normalize_date():
    assert normalize_date("11/05/2024") == "2024-05-11"
    assert normalize_date("11-05-2024") == "2024-05-11"
    assert normalize_date("2024-05-11") == "2024-05-11"
    assert normalize_date("invalid_date") == None
    assert normalize_date("") == None
