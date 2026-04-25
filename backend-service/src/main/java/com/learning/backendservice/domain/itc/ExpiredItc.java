package com.learning.backendservice.domain.itc;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpiredItc(
        String supplierGstin,
        String invoiceNo,
        LocalDate documentDate,
        LocalDate deadline,
        BigDecimal expiredAmount
) {}
