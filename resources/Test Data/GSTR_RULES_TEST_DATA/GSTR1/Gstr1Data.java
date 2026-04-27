package com.gstbuddy.parser.gstr1.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * Gstr1Data
 * ============
 * Strongly-typed data model for the JSON returned by the Python GSTR-1 PDF parser.
 * <p>
 * Structure
 * ---------
 * Gstr1Data
 * ├─ header fields  (form_type, gstin, legal_name, ...)
 * └─ tables : Tables
 * ├─ b2bRegular            : SimpleTable   (section + total)
 * ├─ b2bReverseCharge      : SimpleTable
 * ├─ b2clLarge             : SimpleTable
 * ├─ exports               : ExportsTable  (total + expwp + expwop)
 * ├─ sez                   : SezTable      (total + sezwp + sezwop)
 * ├─ deemedExports         : SimpleTable
 * ├─ b2csOthers            : SimpleTable
 * ├─ nilExemptedNonGst     : NilTable      (total + nil + exempted + non_gst)
 * ├─ amendmentB2bRegular   : AmendmentTable (amended_total + net_differential)
 * ├─ amendmentB2bRc        : AmendmentTable
 * ├─ amendmentB2cl         : AmendmentTable
 * ├─ amendmentExports      : AmendmentExportsTable
 * ├─ amendmentSez          : AmendmentSezTable
 * ├─ amendmentDeemedExports: AmendmentTable
 * ├─ cdnr                  : CdnrTable     (total_net + 4 nested sub-tables)
 * ├─ cdnur                 : CdnurTable    (total_net + unregistered_type)
 * ├─ cdnra                 : CdnraTable    (amended_total + net_differential_total + 4 nested)
 * ├─ cdnura                : CdnuraTable   (amended_total + net_differential_total + unregistered_type)
 * ├─ amendmentB2cs         : AmendmentTable
 * ├─ advancesReceived      : SimpleTable
 * ├─ advancesAdjusted      : SimpleTable
 * ├─ amendmentAdvancesReceived  : AmendmentSimpleTable
 * ├─ amendmentAdvancesAdjusted  : AmendmentSimpleTable
 * ├─ hsnSummary            : SimpleTable
 * ├─ documentsIssued       : DocumentsIssuedTable
 * └─ totalLiability        : TableRow      (flat row at tables level)
 * <p>
 * TableRow — the shared leaf type for every data row:
 * details, no_of_records, document_type, value, igst, cgst, sgst_utgst, cess
 * <p>
 * NestedCdnRow — the intermediate wrapper for CDNR/CDNRA sub-categories:
 * same fields as TableRow (all null) + a net_total TableRow
 * <p>
 * NestedCdnurType — the unregistered_type wrapper in CDNUR/CDNURA:
 * same fields as TableRow (all null) + b2cl, expwp, expwop TableRows
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Gstr1Data {

  @JsonProperty("form_type")
  private String formType;
  @JsonProperty("financial_year")
  private String financialYear;
  @JsonProperty("tax_period")
  private String taxPeriod;
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
  @JsonProperty("tables")
  private Tables tables;

  // =========================================================================
  // Shared leaf — every data row in every table has these fields
  // =========================================================================

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class TableRow {

    @JsonProperty("details")
    private String details;
    @JsonProperty("no_of_records")
    private Long noOfRecords;
    @JsonProperty("document_type")
    private String documentType;
    @JsonProperty("value")
    private Double value;
    @JsonProperty("igst")
    private Double igst;
    @JsonProperty("cgst")
    private Double cgst;
    @JsonProperty("sgst_utgst")
    private Double sgstUtgst;
    @JsonProperty("cess")
    private Double cess;
  }

  // =========================================================================
  // Intermediate wrappers for nested sub-rows in CDNR / CDNRA
  //
  // These objects carry the parent-row fields (all null in practice) plus a
  // net_total child row. Jackson deserialises them without any special config
  // because both the flat fields and the nested child are at the same level.
  // =========================================================================

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class NestedCdnRow {

    @JsonProperty("details")
    private String details;
    @JsonProperty("no_of_records")
    private Long noOfRecords;
    @JsonProperty("document_type")
    private String documentType;
    @JsonProperty("value")
    private Double value;
    @JsonProperty("igst")
    private Double igst;
    @JsonProperty("cgst")
    private Double cgst;
    @JsonProperty("sgst_utgst")
    private Double sgstUtgst;
    @JsonProperty("cess")
    private Double cess;
    @JsonProperty("net_total")
    private TableRow netTotal;
  }

  // Wrapper for the unregistered_type sub-object in CDNUR / CDNURA
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class NestedCdnurType {

    @JsonProperty("details")
    private String details;
    @JsonProperty("no_of_records")
    private Long noOfRecords;
    @JsonProperty("document_type")
    private String documentType;
    @JsonProperty("value")
    private Double value;
    @JsonProperty("igst")
    private Double igst;
    @JsonProperty("cgst")
    private Double cgst;
    @JsonProperty("sgst_utgst")
    private Double sgstUtgst;
    @JsonProperty("cess")
    private Double cess;
    @JsonProperty("b2cl")
    private TableRow b2cl;
    @JsonProperty("expwp")
    private TableRow expwp;
    @JsonProperty("expwop")
    private TableRow expwop;
  }

  // =========================================================================
  // Table types
  // =========================================================================

  /**
   * Tables with only a section label and a single total row (B2B, B2BRC, etc.)
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class SimpleTable {
    @JsonProperty("section")
    private String section;
    @JsonProperty("total")
    private TableRow total;
  }

  /**
   * Tables with section + total + amended_total (advances amendments etc.)
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class AmendmentSimpleTable {
    @JsonProperty("section")
    private String section;
    @JsonProperty("amended_total")
    private TableRow amendedTotal;
    @JsonProperty("total")
    private TableRow total;
  }

  /**
   * Amendment tables with amended_total + net_differential
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class AmendmentTable {
    @JsonProperty("section")
    private String section;
    @JsonProperty("amended_total")
    private TableRow amendedTotal;
    @JsonProperty("net_differential")
    private TableRow netDifferential;
  }

  /**
   * 6A Exports: total + expwp + expwop
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class ExportsTable {
    @JsonProperty("section")
    private String section;
    @JsonProperty("total")
    private TableRow total;
    @JsonProperty("expwp")
    private TableRow expwp;
    @JsonProperty("expwop")
    private TableRow expwop;
  }

  /**
   * 6B SEZ: total + sezwp + sezwop
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class SezTable {
    @JsonProperty("section")
    private String section;
    @JsonProperty("total")
    private TableRow total;
    @JsonProperty("sezwp")
    private TableRow sezwp;
    @JsonProperty("sezwop")
    private TableRow sezwop;
  }

  /**
   * 8 Nil/Exempted/Non-GST: total + nil + exempted + non_gst
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class NilTable {
    @JsonProperty("section")
    private String section;
    @JsonProperty("total")
    private TableRow total;
    @JsonProperty("nil")
    private TableRow nil;
    @JsonProperty("exempted")
    private TableRow exempted;
    @JsonProperty("non_gst")
    private TableRow nonGst;
  }

  /**
   * 9A Amendment exports: amended_total + net_differential_total + expwp + expwop
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class AmendmentExportsTable {
    @JsonProperty("section")
    private String section;
    @JsonProperty("amended_total")
    private TableRow amendedTotal;
    @JsonProperty("net_differential_total")
    private TableRow netDifferentialTotal;
    @JsonProperty("expwp")
    private TableRow expwp;
    @JsonProperty("expwop")
    private TableRow expwop;
  }

  /**
   * 9A Amendment SEZ: amended_total + net_differential_total + sezwp + sezwop
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class AmendmentSezTable {
    @JsonProperty("section")
    private String section;
    @JsonProperty("amended_total")
    private TableRow amendedTotal;
    @JsonProperty("net_differential_total")
    private TableRow netDifferentialTotal;
    @JsonProperty("sezwp")
    private TableRow sezwp;
    @JsonProperty("sezwop")
    private TableRow sezwop;
  }

  /**
   * 9B CDNR: total_net + 4 nested sub-rows each with their own net_total
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class CdnrTable {
    @JsonProperty("section")
    private String section;
    @JsonProperty("total_net")
    private TableRow totalNet;

    @JsonProperty("credit_debit_notes_issued_to_registered_person_for_taxable_outward_supplies_in_b2b_regular")
    private NestedCdnRow b2bRegular;

    @JsonProperty("credit_debit_notes_issued_to_registered_person_for_taxable_outward_supplies_in_b2b_reverse_charge")
    private NestedCdnRow b2bReverseCharge;

    @JsonProperty("credit_debit_notes_issued_to_registered_person_for_taxable_outward_supplies_in_sezwp_sezwop")
    private NestedCdnRow sezwpSezwop;

    @JsonProperty("credit_debit_notes_issued_to_registered_person_for_taxable_outward_supplies_in_de")
    private NestedCdnRow de;
  }

  /**
   * 9B CDNUR: total_net + unregistered_type (with b2cl, expwp, expwop)
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class CdnurTable {
    @JsonProperty("section")
    private String section;
    @JsonProperty("total_net")
    private TableRow totalNet;
    @JsonProperty("unregistered_type")
    private NestedCdnurType unregisteredType;
  }

  /**
   * 9C CDNRA: amended_total + net_differential_total + 4 nested sub-rows
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class CdnraTable {
    @JsonProperty("section")
    private String section;
    @JsonProperty("amended_total")
    private TableRow amendedTotal;
    @JsonProperty("net_differential_total")
    private TableRow netDifferentialTotal;

    @JsonProperty("amended_credit_debit_notes_issued_to_registered_person_for_taxable_outward_b2b_regular")
    private NestedCdnRow b2bRegular;

    @JsonProperty("amended_credit_debit_notes_issued_to_registered_person_for_taxable_outward_b2b_reverse_charge")
    private NestedCdnRow b2bReverseCharge;

    @JsonProperty("amended_credit_debit_notes_issued_to_registered_person_for_taxable_outward_sezwp_sezwop")
    private NestedCdnRow sezwpSezwop;

    @JsonProperty("amended_credit_debit_notes_issued_to_registered_person_for_taxable_outward_de")
    private NestedCdnRow de;
  }

  /**
   * 9C CDNURA: amended_total + net_differential_total + unregistered_type
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class CdnuraTable {
    @JsonProperty("section")
    private String section;
    @JsonProperty("amended_total")
    private TableRow amendedTotal;
    @JsonProperty("net_differential_total")
    private TableRow netDifferentialTotal;
    @JsonProperty("unregistered_type")
    private NestedCdnurType unregisteredType;
  }

  /**
   * 13 Documents issued: section + net_issued_documents row
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class DocumentsIssuedTable {
    @JsonProperty("section")
    private String section;
    @JsonProperty("net_issued_documents")
    private TableRow netIssuedDocuments;
  }

  // =========================================================================
  // Tables — top-level container
  // =========================================================================

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class Tables {

    @JsonProperty("b2b_regular")
    private SimpleTable b2bRegular;

    @JsonProperty("b2b_reverse_charge")
    private SimpleTable b2bReverseCharge;

    @JsonProperty("b2cl_large")
    private SimpleTable b2clLarge;

    @JsonProperty("exports")
    private ExportsTable exports;

    @JsonProperty("sez")
    private SezTable sez;

    @JsonProperty("deemed_exports")
    private SimpleTable deemedExports;

    @JsonProperty("b2cs_others")
    private SimpleTable b2csOthers;

    @JsonProperty("nil_exempted_non_gst")
    private NilTable nilExemptedNonGst;

    @JsonProperty("amendment_b2b_regular")
    private AmendmentTable amendmentB2bRegular;

    @JsonProperty("amendment_b2b_rc")
    private AmendmentTable amendmentB2bRc;

    @JsonProperty("amendment_b2cl")
    private AmendmentTable amendmentB2cl;

    @JsonProperty("amendment_exports")
    private AmendmentExportsTable amendmentExports;

    @JsonProperty("amendment_sez")
    private AmendmentSezTable amendmentSez;

    @JsonProperty("amendment_deemed_exports")
    private AmendmentTable amendmentDeemedExports;

    @JsonProperty("cdnr")
    private CdnrTable cdnr;

    @JsonProperty("cdnur")
    private CdnurTable cdnur;

    @JsonProperty("cdnra")
    private CdnraTable cdnra;

    @JsonProperty("cdnura")
    private CdnuraTable cdnura;

    @JsonProperty("amendment_b2cs")
    private AmendmentTable amendmentB2cs;

    @JsonProperty("advances_received")
    private SimpleTable advancesReceived;

    @JsonProperty("advances_adjusted")
    private SimpleTable advancesAdjusted;

    @JsonProperty("amendment_advances_received")
    private AmendmentSimpleTable amendmentAdvancesReceived;

    @JsonProperty("amendment_advances_adjusted")
    private AmendmentSimpleTable amendmentAdvancesAdjusted;

    @JsonProperty("hsn_summary")
    private SimpleTable hsnSummary;

    @JsonProperty("documents_issued")
    private DocumentsIssuedTable documentsIssued;

    // total_liability sits directly under "tables" as a flat row
    // (no section wrapper — the JSON key maps straight to a TableRow)
    @JsonProperty("total_liability")
    private TableRow totalLiability;
  }
}
