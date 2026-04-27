package com.learning.backendservice.domain.itc;

public enum ItcMismatchType {
    MATCHED,
    MISSING_IN_2B,       // Present in Books, Missing in 2B
    MISSING_IN_BOOKS,    // Present in 2B, Missing in Books
    AMOUNT_MISMATCH,     // Common invoice, but tax values differ
    GSTIN_MISMATCH       // Typo in GSTIN (fuzzy matched, but GSTINs don't perfectly align)
}
