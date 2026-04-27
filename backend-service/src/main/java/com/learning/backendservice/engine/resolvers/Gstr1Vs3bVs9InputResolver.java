package com.learning.backendservice.engine.resolvers;

import com.learning.backendservice.domain.recon.*;
import com.learning.backendservice.engine.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class Gstr1Vs3bVs9InputResolver implements InputResolver<Gstr1Vs3bVs9Input> {

    @Override
    public String getRuleId() {
        return "RECON_1_VS_3B_VS_9";
    }

    @Override
    public Gstr1Vs3bVs9Input resolve(AuditContext context) {
        AuditDocument gstr1Doc = context.getDocument(DocumentType.GSTR_1)
                .orElseThrow(() -> new IllegalStateException("RECON_1_VS_3B_VS_9 requires GSTR_1 document."));
        
        AuditDocument gstr3bDoc = context.getDocument(DocumentType.GSTR_3B)
                .orElseThrow(() -> new IllegalStateException("RECON_1_VS_3B_VS_9 requires GSTR_3B document."));

        AuditDocument gstr9Doc = context.getDocument(DocumentType.GSTR_9)
                .orElseThrow(() -> new IllegalStateException("RECON_1_VS_3B_VS_9 requires GSTR_9 document."));

        if (!context.financialYear().matches("\\d{4}-\\d{2}")) {
            throw new IllegalStateException("Invalid financial year format in context.");
        }

        TaxPaymentSummary gstr1Summary = extractGstr1LiabilityAsTax(gstr1Doc);
        TaxPaymentSummary gstr3bSummary = extractGstr3bTaxPayable(gstr3bDoc);
        TaxPaymentSummary gstr9Table4 = extractGstr9Table4(gstr9Doc);
        TaxPaymentSummary gstr9Table9Paid = extractGstr9Table9Paid(gstr9Doc);

        return new Gstr1Vs3bVs9Input(
                gstr9Doc.gstin() != null ? gstr9Doc.gstin() : gstr1Doc.gstin(),
                context.financialYear(),
                gstr1Summary,
                gstr3bSummary,
                gstr9Table4,
                gstr9Table9Paid,
                context.sharedResources().reconToleranceAmount()
        );
    }

    private TaxPaymentSummary extractGstr1LiabilityAsTax(AuditDocument doc) {
        Map<String, Object> fields = doc.extractedFields();
        Object summaryObj = fields.get("liability_summary");

        if (summaryObj instanceof Map<?, ?> summary) {
            return new TaxPaymentSummary(
                    toBigDecimal(summary.get("total_igst")),
                    toBigDecimal(summary.get("total_cgst")),
                    toBigDecimal(summary.get("total_sgst_utgst")),
                    toBigDecimal(summary.get("total_cess"))
            );
        }
        return TaxPaymentSummary.zero();
    }

    private TaxPaymentSummary extractGstr3bTaxPayable(AuditDocument doc) {
        Map<String, Object> fields = doc.extractedFields();
        Object table61Obj = fields.get("table_6_1");

        if (table61Obj instanceof Map<?, ?> table61) {
            Object taxPayableObj = table61.get("tax_payable");
            if (taxPayableObj instanceof Map<?, ?> taxPayable) {
                return new TaxPaymentSummary(
                        toBigDecimal(taxPayable.get("igst")),
                        toBigDecimal(taxPayable.get("cgst")),
                        toBigDecimal(taxPayable.get("sgst_utgst")),
                        toBigDecimal(taxPayable.get("cess"))
                );
            }
        }
        return TaxPaymentSummary.zero();
    }

    private TaxPaymentSummary extractGstr9Table4(AuditDocument doc) {
        Map<String, Object> fields = doc.extractedFields();
        Object table4Obj = fields.get("table_4");

        if (table4Obj instanceof Map<?, ?> table4) {
            return new TaxPaymentSummary(
                    toBigDecimal(table4.get("igst")),
                    toBigDecimal(table4.get("cgst")),
                    toBigDecimal(table4.get("sgst")),
                    toBigDecimal(table4.get("cess"))
            );
        }
        return TaxPaymentSummary.zero();
    }

    private TaxPaymentSummary extractGstr9Table9Paid(AuditDocument doc) {
        Map<String, Object> fields = doc.extractedFields();
        Object table9Obj = fields.get("table_9");

        if (table9Obj instanceof Map<?, ?> table9) {
            // Paid is sum of tax_paid_cash + tax_paid_itc
            Object cashObj = table9.get("tax_paid_cash");
            Object itcObj = table9.get("tax_paid_itc");
            
            BigDecimal igstCash = BigDecimal.ZERO, cgstCash = BigDecimal.ZERO, sgstCash = BigDecimal.ZERO, cessCash = BigDecimal.ZERO;
            BigDecimal igstItc = BigDecimal.ZERO, cgstItc = BigDecimal.ZERO, sgstItc = BigDecimal.ZERO, cessItc = BigDecimal.ZERO;

            if (cashObj instanceof Map<?, ?> cash) {
                igstCash = toBigDecimal(cash.get("igst"));
                cgstCash = toBigDecimal(cash.get("cgst"));
                sgstCash = toBigDecimal(cash.get("sgst"));
                cessCash = toBigDecimal(cash.get("cess"));
            }
            if (itcObj instanceof Map<?, ?> itc) {
                igstItc = toBigDecimal(itc.get("igst"));
                cgstItc = toBigDecimal(itc.get("cgst"));
                sgstItc = toBigDecimal(itc.get("sgst"));
                cessItc = toBigDecimal(itc.get("cess"));
            }

            return new TaxPaymentSummary(
                    igstCash.add(igstItc),
                    cgstCash.add(cgstItc),
                    sgstCash.add(sgstItc),
                    cessCash.add(cessItc)
            );
        }
        return TaxPaymentSummary.zero();
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Number n) return new BigDecimal(n.toString()).setScale(2, java.math.RoundingMode.HALF_UP);
        try {
            return new BigDecimal(value.toString()).setScale(2, java.math.RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
