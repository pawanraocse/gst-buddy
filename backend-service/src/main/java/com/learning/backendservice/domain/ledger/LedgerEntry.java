package com.learning.backendservice.domain.ledger;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Value object representing a single ledger row from Tally/Busy format.
 * Maps to MVP {@code LedgerEntry} type.
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
    private double amount;

    public enum LedgerEntryType {
        PURCHASE,
        PAYMENT
    }
}
