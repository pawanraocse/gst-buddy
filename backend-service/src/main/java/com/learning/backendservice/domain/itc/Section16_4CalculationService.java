package com.learning.backendservice.domain.itc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Section16_4CalculationService {

    public Section16_4Result evaluate(Section16_4Input input) {
        List<ExpiredItc> expiredRows = new ArrayList<>();
        BigDecimal totalClaimed = BigDecimal.ZERO;
        BigDecimal totalExpired = BigDecimal.ZERO;

        LocalDate claimDate = input.gstr3bFilingDate() != null ? input.gstr3bFilingDate() : input.asOnDate();

        for (Section16_4Input.ItcRow row : input.itcRows()) {
            if (row.invoiceDate() == null) continue;

            BigDecimal rowTotal = zeroIfNull(row.igst())
                    .add(zeroIfNull(row.cgst()))
                    .add(zeroIfNull(row.sgst()))
                    .add(zeroIfNull(row.cess()));

            totalClaimed = totalClaimed.add(rowTotal);

            LocalDate deadline = calculateDeadline(row.invoiceDate(), input.annualReturnDate());

            if (claimDate.isAfter(deadline)) {
                totalExpired = totalExpired.add(rowTotal);
                expiredRows.add(new ExpiredItc(
                        row.supplierGstin(),
                        row.invoiceNo(),
                        row.invoiceDate(),
                        deadline,
                        rowTotal
                ));
            }
        }

        return new Section16_4Result(input.itcRows().size(), totalClaimed, totalExpired, expiredRows);
    }

    private LocalDate calculateDeadline(LocalDate documentDate, LocalDate annualReturnDate) {
        // GST FY: Apr 1 to Mar 31
        int fy = documentDate.getMonthValue() >= 4
                ? documentDate.getYear()
                : documentDate.getYear() - 1;

        // Section 16(4) deadline tiers (as amended by Finance Acts 2022 and 2024):
        //   FY ≥ 2024       → 30-Nov of FY+1  (Finance Act 2024)
        //   FY 2017–2020    → 30-Nov-2021      (Section 16(5) retroactive amnesty)
        //   FY 2021–2023    → 30-Sep of FY+1   (pre-budget 2024; actual deadline is GSTR-3B due date for Sep,
        //                                        but 30-Sep is used as a slightly generous proxy)
        final LocalDate statutoryDeadline;
        if (fy >= 2024) {
            statutoryDeadline = LocalDate.of(fy + 1, 11, 30);
        } else if (fy >= 2017 && fy <= 2020) {
            statutoryDeadline = LocalDate.of(2021, 11, 30);
        } else {
            statutoryDeadline = LocalDate.of(fy + 1, 9, 30);
        }

        // Section 16(4): "whichever is earlier" between statutory deadline and annual return date
        if (annualReturnDate != null && annualReturnDate.isBefore(statutoryDeadline)) {
            return annualReturnDate;
        }

        return statutoryDeadline;
    }

    private BigDecimal zeroIfNull(BigDecimal val) {
        return val == null ? BigDecimal.ZERO : val;
    }
}
