package com.learning.backendservice.engine.resolvers;

import com.learning.backendservice.domain.risk.SupplierRiskInput;
import com.learning.backendservice.engine.AuditContext;
import com.learning.backendservice.engine.AuditDocument;
import com.learning.backendservice.engine.DocumentType;
import com.learning.backendservice.engine.InputResolver;
import com.learning.backendservice.domain.gstr2a.GstinStatusSnapshot;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class SupplierRiskInputResolver implements InputResolver<SupplierRiskInput> {

    public static final String RULE_ID = "SUPPLIER_RISK_2A";

    @Override
    public String getRuleId() {
        return RULE_ID;
    }

    @Override
    @SuppressWarnings("unchecked")
    public SupplierRiskInput resolve(AuditContext context) {
        AuditDocument doc = context.getDocument(DocumentType.GSTR_2A)
                .orElseThrow(() -> new IllegalStateException(
                        "SUPPLIER_RISK_2A requires a GSTR_2A document in context"));

        Map<String, Object> fields = doc.extractedFields();
        String gstin = doc.gstin() != null ? doc.gstin() : context.stateCode();
        List<SupplierRiskInput.SupplierData> suppliers = new ArrayList<>();

        if (fields.get("suppliers") instanceof List<?> rawSuppliers) {
            for (Object obj : rawSuppliers) {
                if (obj instanceof Map<?, ?> supMap) {
                    String supGstin = (String) supMap.get("supplier_gstin");
                    String supName = (String) supMap.get("supplier_name");

                    BigDecimal totalTaxable = BigDecimal.ZERO;
                    BigDecimal totalTax = BigDecimal.ZERO;

                    if (supMap.get("invoices") instanceof List<?> invs) {
                        for (Object iObj : invs) {
                            if (iObj instanceof Map<?, ?> iMap) {
                                // Section 16(2)(c) — RCM invoices EXCLUDED from ITC-at-risk:
                                // If reverse_charge = 'Y', recipient pays tax directly to Govt,
                                // so supplier's filing status does NOT affect ITC eligibility.
                                Object rcm = iMap.get("reverse_charge");
                                if ("Y".equalsIgnoreCase(String.valueOf(rcm)) || Boolean.TRUE.equals(rcm)) {
                                    continue; // Skip RCM invoices
                                }
                                totalTaxable = totalTaxable.add(parseAmount(iMap.get("taxable_value")));
                                totalTax = totalTax.add(parseAmount(iMap.get("igst")))
                                        .add(parseAmount(iMap.get("cgst")))
                                        .add(parseAmount(iMap.get("sgst")))
                                        .add(parseAmount(iMap.get("cess")));
                            }
                        }
                    }

                    GstinStatusSnapshot statusSnapshot = context.sharedResources().gstinStatusMap().get(supGstin);
                    String status = statusSnapshot != null ? statusSnapshot.status() : "ACTIVE";

                    suppliers.add(new SupplierRiskInput.SupplierData(supGstin, supName, status, totalTaxable, totalTax));
                }
            }
        }

        return new SupplierRiskInput(gstin, suppliers);
    }

    private BigDecimal parseAmount(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof Number n) return new BigDecimal(n.toString());
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
