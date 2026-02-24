package com.learning.backendservice.service.export;

import com.learning.backendservice.domain.rule37.LedgerResult;

import java.util.List;

/**
 * Interface for exporting Rule 37 calculation results.
 * Phase 1: Excel. Future: PDF, JSON, CSV.
 */
public interface ExportStrategy {

    /**
     * Generates export bytes from ledger results (issues-only, backward-compatible).
     *
     * @param ledgerResults list of ledger results
     * @param filename      base filename for the export
     * @return export bytes
     */
    byte[] generate(List<LedgerResult> ledgerResults, String filename);

    /**
     * Generates export bytes from ledger results with report type selection.
     *
     * @param ledgerResults list of ledger results
     * @param filename      base filename for the export
     * @param reportType    "issues" (default) or "complete"
     * @return export bytes
     */
    default byte[] generate(List<LedgerResult> ledgerResults, String filename, String reportType) {
        return generate(ledgerResults, filename);
    }

    /**
     * Returns the content type for the export (e.g. application/vnd.openxmlformats-officedocument.spreadsheetml.sheet).
     */
    String getContentType();

    /**
     * Returns the file extension (e.g. xlsx).
     */
    String getFileExtension();
}
