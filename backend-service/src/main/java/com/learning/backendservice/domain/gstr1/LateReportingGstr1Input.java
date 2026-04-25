package com.learning.backendservice.domain.gstr1;

import java.util.List;
import java.time.YearMonth;

/**
 * Immutable input for the GSTR-1 Late Reporting Interest audit rule.
 *
 * <p>Built by {@link com.learning.backendservice.engine.resolvers.LateReportingGstr1InputResolver}
 * from the parsed GSTR-1 document.
 *
 * @param gstin           15-character GSTIN of the taxpayer
 * @param gstr1TaxPeriod  the declared filing period of the GSTR-1 return
 * @param financialYear   GST financial year string, e.g. "2024-25"
 * @param isQrmp          true if the taxpayer is a QRMP (quarterly) filer;
 *                        affects the due date used for delay computation
 * @param invoices        all invoice rows extracted from GSTR-1 Tables 4A and 5A
 */
public record LateReportingGstr1Input(
        String            gstin,
        YearMonth         gstr1TaxPeriod,
        String            financialYear,
        boolean           isQrmp,
        List<InvoiceRow>  invoices
) {}
