package com.learning.backendservice.domain.risk;

import java.math.BigDecimal;
import java.util.List;

public record SupplierRiskInput(
        String taxpayerGstin,
        List<SupplierData> suppliers
) {
    public record SupplierData(
            String gstin,
            String name,
            String status,
            BigDecimal totalTaxableValue,
            BigDecimal totalTax
    ) {}
}
