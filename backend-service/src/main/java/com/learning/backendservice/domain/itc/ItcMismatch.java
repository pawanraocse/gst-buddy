package com.learning.backendservice.domain.itc;

import com.learning.backendservice.domain.shared.PurchaseRegisterRow;
import java.math.BigDecimal;

public record ItcMismatch(
        String invoiceNo,
        String supplierGstin,
        PurchaseRegisterRow booksRow,
        PurchaseRegisterRow gstr2bRow,
        ItcMismatchType type,
        BigDecimal deltaAmount
) {}
