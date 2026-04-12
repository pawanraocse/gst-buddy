package com.learning.backendservice.domain.ledger;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Interface for parsing ledger data from various formats.
 * Phase 1: Excel (Tally/Busy). Future: PDF, CSV.
 *
 * @see LedgerExcelParser
 */
public interface LedgerParser {

    /**
     * Parses ledger entries from the given input stream.
     *
     * @param inputStream the raw file content
     * @param filename    original filename (used for supplier fallback when missing)
     * @return list of ledger entries; never null
     * @throws com.learning.backendservice.exception.LedgerParseException if parsing fails
     */
    List<LedgerEntry> parse(InputStream inputStream, String filename);


    /**
     * Counts the number of distinct ledgers (unique supplier names) in the entry list.
     *
     * @param entries parsed ledger entries; must not be null or empty
     * @return number of distinct supplier names; always ≥ 1 for a non-empty list
     * @throws IllegalArgumentException if entries is null or empty — callers must
     *         validate before invoking; use {@code validateEntries()} in the parser
     */
    default int countLedgers(List<LedgerEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            throw new IllegalArgumentException(
                    "countLedgers requires a non-empty entry list; "
                            + "ensure the file was parsed successfully before calling this method.");
        }

        int count = entries.stream()
                .map(LedgerEntry::getSupplier)
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.toUnmodifiableSet())
                .size();

        if (count == 0) {
            throw new IllegalStateException(
                    "No valid supplier names found in entries; "
                            + "parser fallback supplier should always assign a name.");
        }
        return count;
    }
}

