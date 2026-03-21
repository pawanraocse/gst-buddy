package com.learning.backendservice.domain.ledger;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Value object representing a single ledger row from Tally/Busy format.
 * Maps to MVP {@code LedgerEntry} type.
 * All financial amounts use {@link BigDecimal} for precision (ISSUE-001).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerEntry {

    private LocalDate date;
    private String invoiceNumber;
    private LedgerEntryType entryType;
    private String supplier;
    private BigDecimal amount;

    public enum LedgerEntryType {
        PURCHASE,
        PAYMENT
    }
}
