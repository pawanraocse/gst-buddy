package com.gstbuddy.parser.gstr9.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * Gstr9PdfData
 * ============
 * Strongly-typed model for the JSON returned by the Python GSTR-9 PDF parser.
 *
 * Row types
 * ---------
 *   TaxRow          — sr_no + details + taxable_value + central_tax + state_ut_tax
 *                     + integrated_tax + cess
 *                     Used in: outward_inward_supplies, transactions_declared_in_next_fy,
 *                              other_information (most rows)
 *
 *   ItcRow          — sr_no + details + central_tax + state_ut_tax + integrated_tax + cess
 *                     (no taxable_value)
 *                     Used in: itc_details rows
 *
 *   TaxPaidRow      — sr_no + details + tax_payable + paid_through_cash + paid_through_itc(ItcRow)
 *                     Used in: tax_paid.tax_paid_as_per_returns
 *
 *   DiffRow         — sr_no + details + payable + paid
 *                     Used in: differential_tax_paid, other_information.late_fee
 *
 *   DemandRow       — sr_no + details + central_tax + state_ut_tax + integrated_tax
 *                     + cess + interest + penalty + late_fee_others
 *                     Used in: demands_and_refunds (E, F, G rows)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Gstr9Data {

  @JsonProperty("financial_year")   private String  financialYear;
  @JsonProperty("gstin")            private String  gstin;
  @JsonProperty("legal_name")       private String  legalName;
  @JsonProperty("trade_name")       private String  tradeName;
  @JsonProperty("arn")              private String  arn;
  @JsonProperty("date_of_filing")   private String  dateOfFiling;

  @JsonProperty("outward_inward_supplies")         private OutwardInwardSupplies        outwardInwardSupplies;
  @JsonProperty("itc_details")                     private ItcDetails                   itcDetails;
  @JsonProperty("tax_paid")                        private TaxPaid                      taxPaid;
  @JsonProperty("transactions_declared_in_next_fy")private TransactionsDeclaredNextFy   transactionsDeclaredInNextFy;
  @JsonProperty("other_information")               private OtherInformation             otherInformation;
  @JsonProperty("unknown_tables")                  private Object                       unknownTables;

  // =========================================================================
  // Shared row types
  // =========================================================================

  /** Most supply rows — taxable_value + central/state/integrated/cess */
  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class TaxRow {
    @JsonProperty("sr_no")           private String  srNo;
    @JsonProperty("details")         private String  details;
    @JsonProperty("taxable_value")   private Double  taxableValue;
    @JsonProperty("central_tax")     private Double  centralTax;
    @JsonProperty("state_ut_tax")    private Double  stateUtTax;
    @JsonProperty("integrated_tax")  private Double  integratedTax;
    @JsonProperty("cess")            private Double  cess;
  }

  /** ITC rows — no taxable_value, just tax heads */
  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class ItcRow {
    @JsonProperty("sr_no")           private String  srNo;
    @JsonProperty("details")         private String  details;
    @JsonProperty("central_tax")     private Double  centralTax;
    @JsonProperty("state_ut_tax")    private Double  stateUtTax;
    @JsonProperty("integrated_tax")  private Double  integratedTax;
    @JsonProperty("cess")            private Double  cess;
  }

  /** Tax paid row — tax_payable + cash + itc breakdown */
  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class TaxPaidRow {
    @JsonProperty("sr_no")              private String  srNo;
    @JsonProperty("details")            private String  details;
    @JsonProperty("tax_payable")        private Double  taxPayable;
    @JsonProperty("paid_through_cash")  private Double  paidThroughCash;
    @JsonProperty("paid_through_itc")   private ItcRow  paidThroughItc;
  }

  /** Differential / late-fee rows — payable + paid only */
  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class DiffRow {
    @JsonProperty("sr_no")    private String  srNo;
    @JsonProperty("details")  private String  details;
    @JsonProperty("payable")  private Double  payable;
    @JsonProperty("paid")     private Double  paid;
  }

  /** Demand/refund rows — tax heads + interest + penalty + late_fee_others */
  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class DemandRow {
    @JsonProperty("sr_no")             private String  srNo;
    @JsonProperty("details")           private String  details;
    @JsonProperty("central_tax")       private Double  centralTax;
    @JsonProperty("state_ut_tax")      private Double  stateUtTax;
    @JsonProperty("integrated_tax")    private Double  integratedTax;
    @JsonProperty("cess")              private Double  cess;
    @JsonProperty("interest")          private Double  interest;
    @JsonProperty("penalty")           private Double  penalty;
    @JsonProperty("late_fee_others")   private Double  lateFeeOthers;
  }

  // =========================================================================
  // 4 & 5 — Outward and inward supplies
  // =========================================================================

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class SuppliesOnWhichTaxPayable {
    @JsonProperty("b2c_unregistered")                    private TaxRow b2cUnregistered;
    @JsonProperty("b2b_registered")                      private TaxRow b2bRegistered;
    @JsonProperty("zero_rated_export_with_tax")          private TaxRow zeroRatedExportWithTax;
    @JsonProperty("supplies_to_sez_with_tax")            private TaxRow suppliesToSezWithTax;
    @JsonProperty("deemed_exports")                      private TaxRow deemedExports;
    @JsonProperty("advances_tax_paid_no_invoice")        private TaxRow advancesTaxPaidNoInvoice;
    @JsonProperty("inward_supplies_reverse_charge")      private TaxRow inwardSuppliesReverseCharge;
    @JsonProperty("subtotal_a_to_g")                     private TaxRow subtotalAToG;
    @JsonProperty("credit_notes_b_to_e")                 private TaxRow creditNotesBToE;
    @JsonProperty("debit_notes_b_to_e")                  private TaxRow debitNotesBToE;
    @JsonProperty("supplies_declared_amendments_plus")   private TaxRow suppliesDeclaredAmendmentsPlus;
    @JsonProperty("supplies_reduced_amendments_minus")   private TaxRow suppliesReducedAmendmentsMinus;
    @JsonProperty("subtotal_i_to_l")                     private TaxRow subtotalIToL;
    @JsonProperty("supplies_and_advances_tax_payable")   private TaxRow suppliesAndAdvancesTaxPayable;
    @JsonProperty("unknown_rows")                        private Object unknownRows;
  }

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class SuppliesOnWhichTaxNotPayable {
    @JsonProperty("zero_rated_export_without_tax")       private TaxRow zeroRatedExportWithoutTax;
    @JsonProperty("supply_to_sez_without_tax")           private TaxRow supplyToSezWithoutTax;
    @JsonProperty("reverse_charge_by_recipient")         private TaxRow reverseChargeByRecipient;
    @JsonProperty("exempted")                            private TaxRow exempted;
    @JsonProperty("nil_rated")                           private TaxRow nilRated;
    @JsonProperty("non_gst_supply")                      private TaxRow nonGstSupply;
    @JsonProperty("subtotal_a_to_f")                     private TaxRow subtotalAToF;
    @JsonProperty("credit_notes_a_to_f")                 private TaxRow creditNotesAToF;
    @JsonProperty("debit_notes_a_to_f")                  private TaxRow debitNotesAToF;
    @JsonProperty("supplies_declared_amendments_plus")   private TaxRow suppliesDeclaredAmendmentsPlus;
    @JsonProperty("supplies_reduced_amendments_minus")   private TaxRow suppliesReducedAmendmentsMinus;
    @JsonProperty("subtotal_h_to_k")                     private TaxRow subtotalHToK;
    @JsonProperty("turnover_tax_not_payable")            private TaxRow turnoverTaxNotPayable;
    @JsonProperty("total_turnover_incl_advances")        private TaxRow totalTurnoverInclAdvances;
    @JsonProperty("unknown_rows")                        private Object unknownRows;
  }

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class OutwardInwardSupplies {
    @JsonProperty("supplies_on_which_tax_is_payable")     private SuppliesOnWhichTaxPayable    suppliesOnWhichTaxIsPayable;
    @JsonProperty("supplies_on_which_tax_not_payable")    private SuppliesOnWhichTaxNotPayable suppliesOnWhichTaxNotPayable;
  }

  // =========================================================================
  // 6, 7, 8 — ITC details
  // =========================================================================

  /** Sub-group with inputs / capital_goods / input_services breakdown */
  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class ItcBreakdown {
    @JsonProperty("inputs")          private ItcRow inputs;
    @JsonProperty("capital_goods")   private ItcRow capitalGoods;
    @JsonProperty("input_services")  private ItcRow inputServices;
  }

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class ItcAvailed {
    @JsonProperty("total_itc_availed_gstr3b")              private ItcRow       totalItcAvailedGstr3b;
    @JsonProperty("inward_supplies_excl_imports_rcm")      private ItcBreakdown inwardSuppliesExclImportsRcm;
    @JsonProperty("inward_unregistered_rcm_itc_availed")   private ItcBreakdown inwardUnregisteredRcmItcAvailed;
    @JsonProperty("inward_registered_rcm_itc_availed")     private ItcBreakdown inwardRegisteredRcmItcAvailed;
    @JsonProperty("import_of_goods_incl_sez")              private ItcBreakdown importOfGoodsInclSez;
    @JsonProperty("import_of_services_excl_sez")           private ItcRow       importOfServicesExclSez;
    @JsonProperty("itc_from_isd")                          private ItcRow       itcFromIsd;
    @JsonProperty("itc_reclaimed")                         private ItcRow       itcReclaimed;
    @JsonProperty("subtotal_b_to_h")                       private ItcRow       subtotalBToH;
    @JsonProperty("difference_i_minus_a")                  private ItcRow       differenceIMinusA;
    @JsonProperty("tran1_credit")                          private ItcRow       tran1Credit;
    @JsonProperty("tran2_credit")                          private ItcRow       tran2Credit;
    @JsonProperty("other_itc")                             private ItcRow       otherItc;
    @JsonProperty("subtotal_k_to_m")                       private ItcRow       subtotalKToM;
    @JsonProperty("total_itc_availed")                     private ItcRow       totalItcAvailed;
    @JsonProperty("unknown_rows")                          private Object       unknownRows;
  }

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class ItcReversedIneligible {
    @JsonProperty("rule_37")                       private ItcRow rule37;
    @JsonProperty("rule_39")                       private ItcRow rule39;
    @JsonProperty("rule_42")                       private ItcRow rule42;
    @JsonProperty("rule_43")                       private ItcRow rule43;
    @JsonProperty("section_17_5")                  private ItcRow section175;
    @JsonProperty("reversal_tran1")                private ItcRow reversalTran1;
    @JsonProperty("reversal_tran2")                private ItcRow reversalTran2;
    @JsonProperty("h1_other_reversals")            private ItcRow h1OtherReversals;
    @JsonProperty("total_itc_reversed")            private ItcRow totalItcReversed;
    @JsonProperty("net_itc_available_for_utilization") private ItcRow netItcAvailableForUtilization;
    @JsonProperty("unknown_rows")                  private Object unknownRows;
  }

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class OtherItcInformation {
    @JsonProperty("itc_as_per_gstr2_")                    private ItcRow itcAsPerGstr2;
    @JsonProperty("itc_per_6b_and_6h")                    private ItcRow itcPer6bAnd6h;
    @JsonProperty("itc_availed_next_fy")                   private ItcRow itcAvailedNextFy;
    @JsonProperty("difference_a_minus_b_plus_c")          private ItcRow differenceAMinusBPlusC;
    @JsonProperty("itc_available_not_availed")             private ItcRow itcAvailableNotAvailed;
    @JsonProperty("itc_available_ineligible")              private ItcRow itcAvailableIneligible;
    @JsonProperty("igst_paid_import_of_goods_incl_sez")    private ItcRow igstPaidImportOfGoodsInclSez;
    @JsonProperty("igst_credit_availed_import_goods_6e")   private ItcRow igstCreditAvailedImportGoods6e;
    @JsonProperty("difference_g_minus_h")                  private ItcRow differenceGMinusH;
    @JsonProperty("itc_not_availed_import_goods")          private ItcRow itcNotAvailedImportGoods;
    @JsonProperty("total_itc_to_lapse")                    private ItcRow totalItcToLapse;
    @JsonProperty("unknown_rows")                          private Object unknownRows;
  }

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class ItcDetails {
    @JsonProperty("itc_availed")               private ItcAvailed           itcAvailed;
    @JsonProperty("itc_reversed_ineligible")   private ItcReversedIneligible itcReversedIneligible;
    @JsonProperty("other_itc_information")     private OtherItcInformation  otherItcInformation;
  }

  // =========================================================================
  // 9 — Tax paid
  // =========================================================================

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class TaxPaidAsPerReturns {
    @JsonProperty("integrated_tax")  private TaxPaidRow integratedTax;
    @JsonProperty("central_tax")     private TaxPaidRow centralTax;
    @JsonProperty("state_ut_tax")    private TaxPaidRow stateUtTax;
    @JsonProperty("cess")            private TaxPaidRow cess;
    @JsonProperty("interest")        private TaxPaidRow interest;
    @JsonProperty("late_fee")        private TaxPaidRow lateFee;
    @JsonProperty("penalty")         private TaxPaidRow penalty;
    @JsonProperty("other")           private TaxPaidRow other;
  }

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class TaxPaid {
    @JsonProperty("tax_paid_as_per_returns") private TaxPaidAsPerReturns taxPaidAsPerReturns;
  }

  // =========================================================================
  // 10, 11, 12, 13, 14 — Transactions declared in next FY
  // =========================================================================

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class DifferentialTaxPaid {
    @JsonProperty("integrated_tax")  private DiffRow integratedTax;
    @JsonProperty("central_tax")     private DiffRow centralTax;
    @JsonProperty("state_ut_tax")    private DiffRow stateUtTax;
    @JsonProperty("cess")            private DiffRow cess;
    @JsonProperty("interest")        private DiffRow interest;
  }

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class TransactionsDeclaredNextFy {
    @JsonProperty("supplies_declared_amendments_plus")   private TaxRow             suppliesDeclaredAmendmentsPlus;
    @JsonProperty("supplies_reduced_amendments_minus")   private TaxRow             suppliesReducedAmendmentsMinus;
    @JsonProperty("reversal_itc_previous_fy")            private ItcRow             reversalItcPreviousFy;
    @JsonProperty("itc_availed_previous_fy")             private ItcRow             itcAvailedPreviousFy;
    @JsonProperty("total_turnover_5n_plus_10_minus_11")  private TaxRow             totalTurnover5nPlus10Minus11;
    @JsonProperty("differential_tax_paid")               private DifferentialTaxPaid differentialTaxPaid;
  }

  // =========================================================================
  // 15, 16, 17, 18 — Other information
  // =========================================================================

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class DemandsAndRefunds {
    @JsonProperty("total_refund_claimed")      private ItcRow    totalRefundClaimed;
    @JsonProperty("total_refund_sanctioned")   private ItcRow    totalRefundSanctioned;
    @JsonProperty("total_refund_rejected")     private ItcRow    totalRefundRejected;
    @JsonProperty("total_refund_pending")      private ItcRow    totalRefundPending;
    @JsonProperty("total_demand_of_taxes")     private DemandRow totalDemandOfTaxes;
    @JsonProperty("taxes_paid_against_E")      private DemandRow taxesPaidAgainstE;
    @JsonProperty("demands_pending_from_E")    private DemandRow demandsPendingFromE;
  }

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class CompositionDeemedApproval {
    @JsonProperty("supplies_from_composition_taxpayers")    private TaxRow suppliesFromCompositionTaxpayers;
    @JsonProperty("deemed_supply_section_143")              private TaxRow deemedSupplySection143;
    @JsonProperty("goods_sent_on_approval_not_returned")    private TaxRow goodsSentOnApprovalNotReturned;
  }

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class LateFee {
    @JsonProperty("central_tax")  private DiffRow centralTax;
    @JsonProperty("state_tax")    private DiffRow stateTax;
  }

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class OtherInformation {
    @JsonProperty("demands_and_refunds")          private DemandsAndRefunds        demandsAndRefunds;
    @JsonProperty("composition_deemed_approval")  private CompositionDeemedApproval compositionDeemedApproval;
    @JsonProperty("hsn_summary_outward")          private String                   hsnSummaryOutward;
    @JsonProperty("hsn_summary_inward")           private String                   hsnSummaryInward;
    @JsonProperty("late_fee")                     private LateFee                  lateFee;
  }
}