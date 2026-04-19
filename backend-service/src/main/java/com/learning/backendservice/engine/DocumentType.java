package com.learning.backendservice.engine;

/**
 * Classifies the type of document uploaded into the audit pipeline.
 *
 * <p>The {@link DocumentTypeResolver} assigns one of these types to each file
 * after classification and parsing. The {@link RuleResolutionEngine} uses
 * these types to auto-discover applicable {@link AuditRule} implementations.
 */
public enum DocumentType {

    /** Excel purchase/party ledger — input for Rule 37 (180-day ITC reversal). */
    PURCHASE_LEDGER,

    /** GSTR-1 return — Section 47(1) late fee, Place-of-Supply validation. */
    GSTR_1,

    /** GSTR-3B return — Section 47(2) late fee, Section 50 interest, Rule 86B. */
    GSTR_3B,

    /** GSTR-9 annual return — Section 47(2), year-end reconciliation. */
    GSTR_9,

    /** GSTR-9C reconciliation statement — filed alongside GSTR-9. */
    GSTR_9C,

    /** GSTR-2A auto-populated purchase statement — supplier risk scoring. */
    GSTR_2A,

    /** GSTR-2B static purchase statement — ITC matching, Section 16(4) guard. */
    GSTR_2B,

    /** Excel purchase register — ITC reconciliation, RCM reconciliation. */
    PURCHASE_REGISTER
}
