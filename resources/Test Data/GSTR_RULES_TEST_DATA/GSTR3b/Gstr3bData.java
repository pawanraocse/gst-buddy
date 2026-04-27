package com.gstbuddy.parser.gstr3b.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

/**
 * Gstr3bData
 * =============
 * Strongly-typed data model for the JSON returned by the Python GSTR-3B PDF parser.
 * <p>
 * Structure
 * ---------
 * Gstr3bPdfData
 * ├─ header fields (financial_year, year, month, gstin, legal_name, ...)
 * ├─ outwardInwardSupplies   : OutwardInwardSupplies
 * ├─ ecomSupplies            : SectionOnly          (section only, no rows)
 * ├─ interStateSupplies      : InterStateSupplies
 * ├─ eligibleItc             : EligibleItc
 * ├─ exemptNilNonGstInward   : ExemptNilNonGstInward
 * ├─ interestLateFee         : InterestLateFee
 * ├─ taxPayment              : TaxPayment
 * ├─ tdsTcsCredit            : SectionOnly          (section only, no rows)
 * ├─ systemInterest          : SystemInterest
 * └─ taxLiabilityBreakup     : List<?>              (always empty in observed data)
 * <p>
 * Row types
 * ---------
 * TaxRow          — details + taxable_value + igst + cgst + sgst_utgst + cess
 * used in: outward_inward_supplies, interest_late_fee, eligible_itc
 * InterStateRow   — details + taxable_value + integrated_tax
 * used in: inter_state_supplies
 * ExemptRow       — details + inter_state + intra_state
 * used in: exempt_nil_non_gst_inward
 * TaxPaymentRow   — details + tax_payable + itc_igst + itc_cgst + itc_sgst_utgst
 * + itc_cess + cash_tax + cash_interest + cash_late_fee
 * used in: tax_payment (both other_than_rc and reverse_charge)
 * SystemInterestRow — details + integrated_tax + central_tax + state_ut_tax + cess
 * used in: system_interest
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Gstr3bData {

  @JsonProperty("financial_year")
  private String financialYear;
  @JsonProperty("year")
  private String year;
  @JsonProperty("month")
  private String month;
  @JsonProperty("gstin")
  private String gstin;
  @JsonProperty("legal_name")
  private String legalName;
  @JsonProperty("trade_name")
  private String tradeName;
  @JsonProperty("arn")
  private String arn;
  @JsonProperty("arn_date")
  private String arnDate;
  @JsonProperty("filed_date")
  private String filedDate;

  @JsonProperty("outward_inward_supplies")
  private OutwardInwardSupplies outwardInwardSupplies;
  @JsonProperty("ecom_supplies")
  private EcommSupplies ecomSupplies;
  @JsonProperty("inter_state_supplies")
  private InterStateSupplies interStateSupplies;
  @JsonProperty("eligible_itc")
  private EligibleItc eligibleItc;
  @JsonProperty("exempt_nil_non_gst_inward")
  private ExemptNilNonGstInward exemptNilNonGstInward;
  @JsonProperty("interest_late_fee")
  private InterestLateFee interestLateFee;
  @JsonProperty("tax_payment")
  private TaxPayment taxPayment;
  @JsonProperty("tds_tcs_credit")
  private TdsTcsCredit tdsTcsCredit;
  @JsonProperty("system_interest")
  private SystemInterest systemInterest;
  @JsonProperty("tax_liability_breakup")
  private List<TaxLiabilityItem> taxLiabilityBreakup;

  // =========================================================================
  // Shared row types
  // =========================================================================

  /**
   * Row with taxable_value + igst + cgst + sgst_utgst + cess
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class TaxRow {
    @JsonProperty("details")
    private String details;
    @JsonProperty("igst")
    private Double igst;
    @JsonProperty("cgst")
    private Double cgst;
    @JsonProperty("sgst_utgst")
    private Double sgstUtgst;
    @JsonProperty("cess")
    private Double cess;
    @JsonProperty("taxable_value")
    private Double taxableValue;
  }

  /**
   * Row with only igst + cgst + sgst_utgst + cess (no taxable_value) — used in eligible_itc
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class ItcRow {
    @JsonProperty("details")
    private String details;
    @JsonProperty("igst")
    private Double igst;
    @JsonProperty("cgst")
    private Double cgst;
    @JsonProperty("sgst_utgst")
    private Double sgstUtgst;
    @JsonProperty("cess")
    private Double cess;
  }

  /**
   * Row with taxable_value + integrated_tax — used in inter_state_supplies
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class InterStateRow {
    @JsonProperty("details")
    private String details;
    @JsonProperty("taxable_value")
    private Double taxableValue;
    @JsonProperty("integrated_tax")
    private Double integratedTax;
  }

  /**
   * Row with inter_state + intra_state — used in exempt_nil_non_gst_inward
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class ExemptRow {
    @JsonProperty("details")
    private String details;
    @JsonProperty("inter_state")
    private Double interState;
    @JsonProperty("intra_state")
    private Double intraState;
  }

  /**
   * Row used in tax_payment — tax_payable + itc breakdown + cash breakdown
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class TaxPaymentRow {
    @JsonProperty("details")
    private String details;
    @JsonProperty("tax_payable")
    private Double taxPayable;
    @JsonProperty("interest_payable")
    private Double interestPayable;   // newer format
    @JsonProperty("late_fee_payable")
    private Double lateFeePayable;    // newer format
    @JsonProperty("itc_igst")
    private Double itcIgst;
    @JsonProperty("itc_cgst")
    private Double itcCgst;
    @JsonProperty("itc_sgst_utgst")
    private Double itcSgstUtgst;
    @JsonProperty("itc_cess")
    private Double itcCess;
    @JsonProperty("cash_tax")
    private Double cashTax;
    @JsonProperty("cash_interest")
    private Double cashInterest;
    @JsonProperty("cash_late_fee")
    private Double cashLateFee;
    @JsonProperty("tds_tcs")
    private Double tdsTcs;            // newer format
  }

  /**
   * Grand total row in tax_payment — different shape from TaxPaymentRow
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class GrandTotalRow {
    @JsonProperty("details")
    private String details;
    @JsonProperty("tax_payable")
    private Double taxPayable;
    @JsonProperty("itc_paid")
    private Double itcPaid;
    @JsonProperty("cash_tax")
    private Double cashTax;
    @JsonProperty("tds_tcs")
    private Double tdsTcs;
  }


  // =========================================================================
  // Section-only placeholder (section label present, no data rows)
  // =========================================================================

  /**
   * 3.1.1 E-commerce supplies (newer format — FY 2024-25+)
   * Older format had ecom_supplies as SectionOnly (section label only, no rows).
   * Newer format has two TaxRow entries under the section.
   * Both formats deserialise cleanly since all fields are nullable.
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class EcommSupplies {
    @JsonProperty("section")
    private String section;
    @JsonProperty("ecom_pays_tax")
    private TaxRow ecomPaysTax;
    @JsonProperty("registered_through_ecom")
    private TaxRow registeredThroughEcom;
  }

  // =========================================================================
  // Section classes
  // =========================================================================

  /**
   * 3.1 Outward and inward supplies liable to reverse charge
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class OutwardInwardSupplies {
    @JsonProperty("section")
    private String section;
    @JsonProperty("outward_taxable_other")
    private TaxRow outwardTaxableOther;
    @JsonProperty("outward_taxable_zero_rated")
    private TaxRow outwardTaxableZeroRated;
    @JsonProperty("other_outward_nil_exempt")
    private TaxRow otherOutwardNilExempt;
    @JsonProperty("inward_reverse_charge")
    private TaxRow inwardReverseCharge;
    @JsonProperty("non_gst_outward")
    private TaxRow nonGstOutward;
    @JsonProperty("total")
    private TaxRow total;           // newer format only
  }

  /**
   * 3.2 Inter-state supplies to unregistered/composition/UIN
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class InterStateSupplies {
    @JsonProperty("section")
    private String section;
    @JsonProperty("unregistered_persons")
    private InterStateRow unregisteredPersons;
    @JsonProperty("composition_taxable_persons")
    private InterStateRow compositionTaxablePersons;
    @JsonProperty("uin_holders")
    private InterStateRow uinHolders;
  }

  /**
   * 4 Eligible ITC
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class EligibleItc {
    @JsonProperty("section")
    private String section;
    @JsonProperty("itc_available")
    private ItcRow itcAvailable;
    @JsonProperty("import_of_goods")
    private ItcRow importOfGoods;
    @JsonProperty("import_of_services")
    private ItcRow importOfServices;
    @JsonProperty("inward_rcm")
    private ItcRow inwardRcm;
    @JsonProperty("inward_isd")
    private ItcRow inwardIsd;
    @JsonProperty("all_other_itc")
    private ItcRow allOtherItc;
    @JsonProperty("itc_reversed")
    private ItcRow itcReversed;
    @JsonProperty("reversed_rules_42_43")
    private ItcRow reversedRules4243;
    @JsonProperty("reversed_others")
    private ItcRow reversedOthers;
    @JsonProperty("net_itc_available")
    private ItcRow netItcAvailable;
    @JsonProperty("ineligible_itc")
    private ItcRow ineligibleItc;
    @JsonProperty("ineligible_itc_reclaimed")
    private ItcRow ineligibleItcReclaimed;
    @JsonProperty("ineligible_section_16_4")
    private ItcRow ineligibleSection164;
    @JsonProperty("ineligible_17_5")
    private ItcRow ineligible175;           // newer format
    @JsonProperty("ineligible_others")
    private ItcRow ineligibleOthers;        // newer format
  }

  /**
   * 5 Exempt, nil-rated and non-GST inward supplies
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class ExemptNilNonGstInward {
    @JsonProperty("section")
    private String section;
    @JsonProperty("composition_exempt_nil")
    private ExemptRow compositionExemptNil;
    @JsonProperty("non_gst_supply")
    private ExemptRow nonGstSupply;
  }

  /**
   * 5.1 Interest and late fee
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class InterestLateFee {
    @JsonProperty("section")
    private String section;
    @JsonProperty("system_computed_interest")
    private ItcRow sysComputedInterest;
    @JsonProperty("interest_paid")
    private ItcRow interestPaid;
    @JsonProperty("interest")
    private ItcRow interest;
    @JsonProperty("late_fee")
    private ItcRow lateFee;
  }

  /**
   * 6.1 Payment of tax
   * <p>
   * Both other_than_reverse_charge and reverse_charge have the same shape:
   * four TaxPaymentRow entries keyed by igst / cgst / sgst_utgst / cess.
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class TaxPaymentGroup {
    @JsonProperty("igst")
    private TaxPaymentRow igst;
    @JsonProperty("cgst")
    private TaxPaymentRow cgst;
    @JsonProperty("sgst_utgst")
    private TaxPaymentRow sgstUtgst;
    @JsonProperty("cess")
    private TaxPaymentRow cess;
    @JsonProperty("total")
    private TaxPaymentRow total;    // newer format only
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class TaxPayment {
    @JsonProperty("section")
    private String section;
    @JsonProperty("other_than_reverse_charge")
    private TaxPaymentGroup otherThanReverseCharge;
    @JsonProperty("reverse_charge")
    private TaxPaymentGroup reverseCharge;
    @JsonProperty("grand_total")
    private GrandTotalRow grandTotal;
  }

  /**
   * System-computed interest row
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class SystemInterestRow {
    @JsonProperty("details")
    private String details;
    @JsonProperty("integrated_tax")
    private Double integratedTax;
    @JsonProperty("central_tax")
    private Double centralTax;
    @JsonProperty("state_ut_tax")
    private Double stateUtTax;
    @JsonProperty("cess")
    private Double cess;
  }

  /**
   * System interest — section only in NEW1, has interest row in OLD and NEW2
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class SystemInterest {
    @JsonProperty("section")
    private String section;
    @JsonProperty("interest")
    private SystemInterestRow interest;
  }

  /**
   * Row for tds_tcs_credit — igst + cgst + sgst_utgst (no cess)
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class TdsTcsRow {
    @JsonProperty("details")
    private String details;
    @JsonProperty("igst")
    private Double igst;
    @JsonProperty("cgst")
    private Double cgst;
    @JsonProperty("sgst_utgst")
    private Double sgstUtgst;
  }

  /**
   * 6.2 TDS/TCS Credit — section only in older format, has rows in newer format
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class TdsTcsCredit {
    @JsonProperty("section")
    private String section;
    @JsonProperty("tds")
    private TdsTcsRow tds;
    @JsonProperty("tcs")
    private TdsTcsRow tcs;
    @JsonProperty("total")
    private TdsTcsRow total;
  }

  /**
   * One item in the tax_liability_breakup list
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class TaxLiabilityItem {
    @JsonProperty("period")
    private String period;
    @JsonProperty("integrated_tax")
    private Double integratedTax;
    @JsonProperty("central_tax")
    private Double centralTax;
    @JsonProperty("state_ut_tax")
    private Double stateUtTax;
    @JsonProperty("cess")
    private Double cess;
  }
}