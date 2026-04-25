package com.learning.backendservice.domain.itc;

import java.math.BigDecimal;
import java.util.List;

public record Section16_4Result(
        int totalRowsChecked,
        BigDecimal totalItcClaimed,
        BigDecimal totalExpiredItc,
        List<ExpiredItc> expiredRows
) {}
