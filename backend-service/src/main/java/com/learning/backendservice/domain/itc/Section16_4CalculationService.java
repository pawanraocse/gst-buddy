package com.learning.backendservice.domain.itc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Evaluates ITC eligibility based on Section 16(4) of the CGST Act, 2017.
 * 
 * <p>Validates if the ITC claimed is within the permitted statutory deadlines:
 * <ul>
 *   <li><b>FY 2024-25 onwards:</b> 30th November of the succeeding financial year.</li>
 *   <li><b>FY 2017-18 to 2020-21:</b> Amnesty extended the deadline up to 30th November 2021 (Section 16(5)).</li>
 *   <li><b>FY 2021-22 to 2023-24:</b> 20th October of the succeeding financial year (due date for filing September GSTR-3B).</li>
 * </ul>
 * 
 * <p>The deadline is the statutory deadline or the date of furnishing the annual return (GSTR-9), whichever is earlier.
 */
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
        //   FY 2021–2023    → 20-Oct of FY+1   (due date for filing September GSTR-3B)
        final LocalDate statutoryDeadline;
        if (fy >= 2024) {
            statutoryDeadline = LocalDate.of(fy + 1, 11, 30);
        } else if (fy >= 2017 && fy <= 2020) {
            statutoryDeadline = LocalDate.of(2021, 11, 30);
        } else {
            statutoryDeadline = LocalDate.of(fy + 1, 10, 20);
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
