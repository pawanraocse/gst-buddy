package com.learning.backendservice.domain.gstr1;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Immutable input for the GSTR-1 Late Fee audit rule.
 *
 * <p>Built by {@code AuditRunOrchestrator} by combining:
 * <ol>
 *   <li>Data extracted from the document by the Python parser sidecar
 *       ({@code gstin}, {@code arnDate}, {@code taxPeriod}).</li>
 *   <li>CA-supplied flags from the UI ({@code isNilReturn}, {@code isQrmp}),
 *       optionally auto-detected by the parser.</li>
 *   <li>A pre-resolved {@link ReliefWindowSnapshot} queried from the DB
 *       (may be {@code null} if no relief window applies).</li>
 * </ol>
 *
 * @param gstin            15-character GSTIN of the taxpayer
 * @param arnDate          ARN date extracted from the GSTR-1 document (actual filing date)
 * @param taxPeriod        tax period of this return (e.g. {@code YearMonth.of(2024, 3)} = Mar-2024)
 * @param financialYear    GST financial year string, e.g. "2024-25"
 * @param isNilReturn      true if the GSTR-1 is a nil-return filing
 * @param isQrmp           true if the taxpayer is a QRMP (Quarterly) filer
 * @param reliefWindow     pre-resolved amnesty/waiver snapshot, or {@code null} if none applies
 */
public record Gstr1LateFeeInput(
        String               gstin,
        LocalDate            arnDate,
        YearMonth            taxPeriod,
        String               financialYear,
        boolean              isNilReturn,
        boolean              isQrmp,
        ReliefWindowSnapshot reliefWindow
) {}
