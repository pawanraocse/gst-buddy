package com.gstbuddy.parser.gstr9c.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

/**
 * Gstr9cPdfData
 * =============
 * Strongly-typed model for the JSON returned by the Python GSTR-9C PDF parser.
 *
 * Row types
 * ---------
 *   AmountRow       — sr_no + details + sign? + amount
 *                     Used in: turnover_reconciliation (gross_turnover, taxable_turnover,
 *                              net_itc), itc_reconciliation.tax_payable_on_itc_diff
 *
 *   RateRow         — sr_no + details + taxable_value? + cgst + sgst + igst + cess
 *                     Used in: rate_wise_liability, additional_payable, liability_by_rate
 *
 *   ExpenseRow      — sr_no + details + value + total_itc + eligible_itc
 *                     Used in: itc_by_expense_head
 *
 *   DiffRow         — sr_no + details + payable + paid
 *                     Used in: late_fee sections
 *
 *   PayableRow      — sr_no + details + amount_payable
 *                     Used in: tax_payable_on_itc_diff
 *
 *   ReasonItem      — sr_no + reason + details  (list items)
 *                     Used in all reasons_* list fields
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Gstr9cData {

  @JsonProperty("financial_year")  private String               financialYear;
  @JsonProperty("gstin")           private String               gstin;
  @JsonProperty("legal_name")      private String               legalName;
  @JsonProperty("trade_name")      private String               tradeName;
  @JsonProperty("arn")             private String               arn;
  @JsonProperty("arn_date")        private String               arnDate;
  @JsonProperty("audit_act")       private String               auditAct;

  @JsonProperty("turnover_reconciliation") private TurnoverReconciliation turnoverReconciliation;
  @JsonProperty("tax_paid_reconciliation") private TaxPaidReconciliation  taxPaidReconciliation;
  @JsonProperty("itc_reconciliation")      private ItcReconciliation      itcReconciliation;
  @JsonProperty("additional_liability")    private AdditionalLiability    additionalLiability;

  // =========================================================================
  // Shared row types
  // =========================================================================

  /** Turnover/ITC reconciliation rows — sign indicator + single amount */
  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class AmountRow {
    @JsonProperty("sr_no")    private String  srNo;
    @JsonProperty("details")  private String  details;
    @JsonProperty("sign")     private String  sign;
    @JsonProperty("amount")   private Double  amount;
  }

  /** Rate-wise tax rows — value? + taxable_value? + cgst + sgst + igst + cess
   *  The "value" field appears in additional_liability.liability_by_rate rows.
   *  The "taxable_value" field appears in tax_paid_reconciliation rows.
   *  Both are optional (nullable) so a single class covers both contexts. */
  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class RateRow {
    @JsonProperty("sr_no")          private String  srNo;
    @JsonProperty("details")        private String  details;
    @JsonProperty("value")          private Double  value;
    @JsonProperty("taxable_value")  private Double  taxableValue;
    @JsonProperty("cgst")           private Double  cgst;
    @JsonProperty("sgst")           private Double  sgst;
    @JsonProperty("igst")           private Double  igst;
    @JsonProperty("cess")           private Double  cess;
  }

  /** Expense-head ITC rows — value + total_itc + eligible_itc */
  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class ExpenseRow {
    @JsonProperty("sr_no")         private String  srNo;
    @JsonProperty("details")       private String  details;
    @JsonProperty("value")         private Double  value;
    @JsonProperty("total_itc")     private Double  totalItc;
    @JsonProperty("eligible_itc")  private Double  eligibleItc;
  }

  /** Late-fee rows — payable + paid */
  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class DiffRow {
    @JsonProperty("sr_no")    private String  srNo;
    @JsonProperty("details")  private String  details;
    @JsonProperty("payable")  private Double  payable;
    @JsonProperty("paid")     private Double  paid;
  }

  /** ITC diff payable rows — single amount_payable field */
  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class PayableRow {
    @JsonProperty("sr_no")           private String  srNo;
    @JsonProperty("details")         private String  details;
    @JsonProperty("amount_payable")  private Double  amountPayable;
  }

  /** List item for all reasons_* fields */
  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class ReasonItem {
    @JsonProperty("sr_no")    private String  srNo;
    @JsonProperty("reason")   private String  reason;
    @JsonProperty("details")  private String  details;
  }

  // =========================================================================
  // 5 — Turnover reconciliation
  // =========================================================================

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class GrossTurnover {
    @JsonProperty("turnover_as_per_audited_financial_statements") private AmountRow turnoverAsPerAuditedFinancialStatements;
    @JsonProperty("unbilled_revenue_beginning_fy")                private AmountRow unbilledRevenueBeginningFy;
    @JsonProperty("unadjusted_advances_end_fy")                   private AmountRow unadjustedAdvancesEndFy;
    @JsonProperty("deemed_supply_schedule_i")                     private AmountRow deemedSupplyScheduleI;
    @JsonProperty("credit_notes_after_end_fy")                    private AmountRow creditNotesAfterEndFy;
    @JsonProperty("trade_discounts_not_permissible")              private AmountRow tradeDiscountsNotPermissible;
    @JsonProperty("turnover_april_to_june_2017")                  private AmountRow turnoverAprilToJune2017;
    @JsonProperty("unbilled_revenue_end_fy")                      private AmountRow unbilledRevenueEndFy;
    @JsonProperty("unadjusted_advances_beginning_fy")             private AmountRow unadjustedAdvancesBeginningFy;
    @JsonProperty("credit_notes_not_permissible")                 private AmountRow creditNotesNotPermissible;
    @JsonProperty("sez_to_dta_adjustments")                       private AmountRow sezToDtaAdjustments;
    @JsonProperty("turnover_under_composition")                   private AmountRow turnoverUnderComposition;
    @JsonProperty("adjustments_section_15")                       private AmountRow adjustmentsSection15;
    @JsonProperty("adjustments_foreign_exchange")                 private AmountRow adjustmentsForeignExchange;
    @JsonProperty("adjustments_other_reasons")                    private AmountRow adjustmentsOtherReasons;
    @JsonProperty("annual_turnover_after_adjustments")            private AmountRow annualTurnoverAfterAdjustments;
    @JsonProperty("turnover_as_per_annual_return_gstr9")          private AmountRow turnoverAsPerAnnualReturnGstr9;
    @JsonProperty("unreconciled_turnover")                        private AmountRow unreconciledTurnover;
  }

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class TaxableTurnover {
    @JsonProperty("annual_turnover_after_adjustments")        private AmountRow annualTurnoverAfterAdjustments;
    @JsonProperty("exempted_nil_non_gst_no_supply")           private AmountRow exemptedNilNonGstNoSupply;
    @JsonProperty("zero_rated_without_payment")               private AmountRow zeroRatedWithoutPayment;
    @JsonProperty("reverse_charge_supplies")                  private AmountRow reverseChargeSupplies;
    @JsonProperty("taxable_turnover_as_per_adjustments")      private AmountRow taxableTurnoverAsPerAdjustments;
    @JsonProperty("taxable_turnover_as_per_annual_return")    private AmountRow taxableTurnoverAsPerAnnualReturn;
    @JsonProperty("unreconciled_taxable_turnover")            private AmountRow unreconciledTaxableTurnover;
  }

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class TurnoverReconciliation {
    @JsonProperty("gross_turnover")                           private GrossTurnover         grossTurnover;
    @JsonProperty("reasons_gross_turnover_unreconciled")      private List<ReasonItem>      reasonsGrossTurnoverUnreconciled;
    @JsonProperty("taxable_turnover")                         private TaxableTurnover       taxableTurnover;
    @JsonProperty("reasons_taxable_turnover_unreconciled")    private List<ReasonItem>      reasonsTaxableTurnoverUnreconciled;
  }

  // =========================================================================
  // 9 — Tax paid reconciliation
  // =========================================================================

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class RateWiseLiability {
    @JsonProperty("rate_5pct")                          private RateRow rate5pct;
    @JsonProperty("rate_5pct_rc")                       private RateRow rate5pctRc;
    @JsonProperty("rate_6pct")                          private RateRow rate6pct;
    @JsonProperty("rate_12pct")                         private RateRow rate12pct;
    @JsonProperty("rate_12pct_rc")                      private RateRow rate12pctRc;
    @JsonProperty("rate_18pct")                         private RateRow rate18pct;
    @JsonProperty("rate_18pct_rc")                      private RateRow rate18pctRc;
    @JsonProperty("rate_28pct")                         private RateRow rate28pct;
    @JsonProperty("rate_28pct_rc")                      private RateRow rate28pctRc;
    @JsonProperty("rate_3pct")                          private RateRow rate3pct;
    @JsonProperty("rate_0_25pct")                       private RateRow rate025pct;
    @JsonProperty("rate_0_10pct")                       private RateRow rate010pct;
    @JsonProperty("rate_others")                        private RateRow rateOthers;
    @JsonProperty("interest")                           private RateRow interest;
    @JsonProperty("late_fee")                           private RateRow lateFee;
    @JsonProperty("penalty")                            private RateRow penalty;
    @JsonProperty("others")                             private RateRow others;
    @JsonProperty("total_amount_to_be_paid")            private RateRow totalAmountToBePaid;
    @JsonProperty("total_amount_paid_as_per_annual_return") private RateRow totalAmountPaidAsPerAnnualReturn;
    @JsonProperty("unreconciled_payment")               private RateRow unreconciledPayment;
  }

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class AdditionalAmountPayable {
    @JsonProperty("rate_5pct")       private RateRow rate5pct;
    @JsonProperty("rate_6pct")       private RateRow rate6pct;
    @JsonProperty("rate_12pct")      private RateRow rate12pct;
    @JsonProperty("rate_18pct")      private RateRow rate18pct;
    @JsonProperty("rate_28pct")      private RateRow rate28pct;
    @JsonProperty("rate_3pct")       private RateRow rate3pct;
    @JsonProperty("rate_0_25pct")    private RateRow rate025pct;
    @JsonProperty("rate_0_10pct")    private RateRow rate010pct;
    @JsonProperty("rate_others")     private RateRow rateOthers;
    @JsonProperty("interest")        private RateRow interest;
    @JsonProperty("late_fee")        private RateRow lateFee;
    @JsonProperty("penalty")         private RateRow penalty;
    @JsonProperty("others")          private RateRow others;
  }

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class TaxPaidReconciliation {
    @JsonProperty("rate_wise_liability")                private RateWiseLiability      rateWiseLiability;
    @JsonProperty("reasons_payment_unreconciled")       private List<ReasonItem>       reasonsPaymentUnreconciled;
    @JsonProperty("additional_amount_payable")          private AdditionalAmountPayable additionalAmountPayable;
  }

  // =========================================================================
  // 12 — ITC reconciliation
  // =========================================================================

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class NetItc {
    @JsonProperty("itc_availed_per_audited_financial_statement")      private AmountRow itcAvailedPerAuditedFinancialStatement;
    @JsonProperty("itc_booked_earlier_fy_claimed_current_fy")         private AmountRow itcBookedEarlierFyClaimedCurrentFy;
    @JsonProperty("itc_booked_current_fy_claimed_subsequent_fy")      private AmountRow itcBookedCurrentFyClaimedSubsequentFy;
    @JsonProperty("itc_availed_per_books_of_account")                 private AmountRow itcAvailedPerBooksOfAccount;
    @JsonProperty("itc_claimed_in_annual_return_gstr9")               private AmountRow itcClaimedInAnnualReturnGstr9;
    @JsonProperty("unreconciled_itc")                                 private AmountRow unreconciledItc;
  }

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class ItcByExpenseHead {
    @JsonProperty("purchases")                private ExpenseRow purchases;
    @JsonProperty("freight_carriage")         private ExpenseRow freightCarriage;
    @JsonProperty("power_and_fuel")           private ExpenseRow powerAndFuel;
    @JsonProperty("imported_goods")           private ExpenseRow importedGoods;
    @JsonProperty("rent_and_insurance")       private ExpenseRow rentAndInsurance;
    @JsonProperty("goods_lost_destroyed")     private ExpenseRow goodsLostDestroyed;
    @JsonProperty("royalties")                private ExpenseRow royalties;
    @JsonProperty("employees_cost")           private ExpenseRow employeesCost;
    @JsonProperty("conveyance_charges")       private ExpenseRow conveyanceCharges;
    @JsonProperty("bank_charges")             private ExpenseRow bankCharges;
    @JsonProperty("entertainment_charges")    private ExpenseRow entertainmentCharges;
    @JsonProperty("stationery_expenses")      private ExpenseRow stationeryExpenses;
    @JsonProperty("repair_and_maintenance")   private ExpenseRow repairAndMaintenance;
    @JsonProperty("other_miscellaneous")      private ExpenseRow otherMiscellaneous;
    @JsonProperty("capital_goods")            private ExpenseRow capitalGoods;
    @JsonProperty("any_other_expense_1")      private ExpenseRow anyOtherExpense1;
    @JsonProperty("any_other_expense_2")      private ExpenseRow anyOtherExpense2;
    @JsonProperty("any_other_expense_3")      private ExpenseRow anyOtherExpense3;
    @JsonProperty("any_other_expense_4")      private ExpenseRow anyOtherExpense4;
    @JsonProperty("any_other_expense_5")      private ExpenseRow anyOtherExpense5;
    @JsonProperty("any_other_expense_6")      private ExpenseRow anyOtherExpense6;
    @JsonProperty("any_other_expense_7")      private ExpenseRow anyOtherExpense7;
    @JsonProperty("any_other_expense_8")      private ExpenseRow anyOtherExpense8;
    @JsonProperty("any_other_expense_9")      private ExpenseRow anyOtherExpense9;
    @JsonProperty("any_other_expense_10")     private ExpenseRow anyOtherExpense10;
    @JsonProperty("total_eligible_itc_availed") private ExpenseRow totalEligibleItcAvailed;
    @JsonProperty("itc_claimed_in_annual_return") private ExpenseRow itcClaimedInAnnualReturn;
    @JsonProperty("unreconciled_itc")         private ExpenseRow unreconciledItc;
  }

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class TaxPayableOnItcDiff {
    @JsonProperty("central_tax")     private PayableRow centralTax;
    @JsonProperty("state_ut_tax")    private PayableRow stateUtTax;
    @JsonProperty("integrated_tax")  private PayableRow integratedTax;
    @JsonProperty("cess")            private PayableRow cess;
    @JsonProperty("interest")        private PayableRow interest;
    @JsonProperty("penalty")         private PayableRow penalty;
  }

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class ItcReconciliation {
    @JsonProperty("net_itc")                              private NetItc               netItc;
    @JsonProperty("reasons_net_itc_unreconciled")         private List<ReasonItem>     reasonsNetItcUnreconciled;
    @JsonProperty("itc_by_expense_head")                  private ItcByExpenseHead     itcByExpenseHead;
    @JsonProperty("reasons_expense_itc_unreconciled")     private List<ReasonItem>     reasonsExpenseItcUnreconciled;
    @JsonProperty("tax_payable_on_itc_diff")              private TaxPayableOnItcDiff  taxPayableOnItcDiff;
  }

  // =========================================================================
  // 14 — Additional liability
  // =========================================================================

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class LiabilityByRate {
    @JsonProperty("rate_5pct")              private RateRow rate5pct;
    @JsonProperty("rate_6pct")              private RateRow rate6pct;
    @JsonProperty("rate_12pct")             private RateRow rate12pct;
    @JsonProperty("rate_18pct")             private RateRow rate18pct;
    @JsonProperty("rate_28pct")             private RateRow rate28pct;
    @JsonProperty("rate_40pct")             private RateRow rate40pct;
    @JsonProperty("rate_3pct")              private RateRow rate3pct;
    @JsonProperty("rate_0_25pct")           private RateRow rate025pct;
    @JsonProperty("rate_0_10pct")           private RateRow rate010pct;
    @JsonProperty("rate_others")            private RateRow rateOthers;
    @JsonProperty("input_tax_credit")       private RateRow inputTaxCredit;
    @JsonProperty("interest")               private RateRow interest;
    @JsonProperty("late_fee")               private RateRow lateFee;
    @JsonProperty("penalty")                private RateRow penalty;
    @JsonProperty("other_amount_not_in_ar") private RateRow otherAmountNotInAr;
    @JsonProperty("erroneous_refund")       private RateRow erroneousRefund;
    @JsonProperty("outstanding_demands")    private RateRow outstandingDemands;
    @JsonProperty("other")                  private RateRow other;
  }

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class LateFee {
    @JsonProperty("central_tax")  private DiffRow centralTax;
    @JsonProperty("state_tax")    private DiffRow stateTax;
  }

  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class AdditionalLiability {
    @JsonProperty("liability_by_rate")  private LiabilityByRate liabilityByRate;
    @JsonProperty("late_fee")           private LateFee         lateFee;
  }
}
