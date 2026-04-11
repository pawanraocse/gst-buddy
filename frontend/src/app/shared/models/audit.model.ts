/**
 * Generic Audit Engine models — Phase 5 migration.
 *
 * Replaces rule37.model.ts.
 * All run IDs are UUID v7 strings (replaces numeric BIGSERIAL).
 * Backend source: AuditRunResponse.java, UploadResult.java
 */

// ─────────────────────────────────────────────
//  Rule 37 domain types (UNCHANGED — pure domain)
// ─────────────────────────────────────────────

export type RiskCategory = 'SAFE' | 'AT_RISK' | 'BREACHED';

export interface InterestRow {
  supplier: string;
  invoiceNumber: string | null;
  purchaseDate: string;
  paymentDate: string | null;
  originalInvoiceValue: number;
  principal: number;
  delayDays: number;
  itcAmount: number;
  interest: number;
  status: 'PAID_LATE' | 'PAID_ON_TIME' | 'UNPAID';
  paymentDeadline: string;
  riskCategory: RiskCategory;
  gstr3bPeriod: string;
  daysToDeadline: number;
  itcAvailmentDate: string | null;
}

export interface CalculationSummary {
  totalInterest: number;
  totalItcReversal: number;
  details: InterestRow[];
  atRiskCount: number;
  atRiskAmount: number;
  breachedCount: number;
  calculationDate: string;
}

export interface LedgerResult {
  ledgerName: string;
  summary: CalculationSummary;
}

// ─────────────────────────────────────────────
//  Upload response (POST /api/v1/ledgers/upload)
// ─────────────────────────────────────────────

export interface UploadResult {
  /** UUID v7 string — primary identifier for all new runs */
  stringRunId: string;
  /** @deprecated Use stringRunId. Kept for one-version backward compat. */
  runId?: number;
  filename: string;
  results: { ledgerName: string; summary: CalculationSummary }[];
  errors: { filename: string; message: string }[];
  creditsConsumed: number;
  remainingCredits: number;
}

// ─────────────────────────────────────────────
//  Audit Run (GET /api/v1/audit/runs)
// ─────────────────────────────────────────────

/** Maps to backend AuditRunResponse.java */
export interface AuditRunResponse {
  /** UUID v7 string */
  runId: string;
  ruleId: string;
  ruleDisplayName: string;
  /** PENDING | RUNNING | SUCCESS | FAILED | COMPLETED */
  status: string;
  totalImpactAmount: number;
  creditsConsumed: number;
  createdAt: string;
  completedAt: string | null;
  expiresAt: string;
  userId: string;
  /**
   * Rule-specific input params (e.g. { asOnDate, filename }).
   * Present in all list and detail responses.
   */
  inputMetadata: AuditRunInputMetadata | null;
  /**
   * High-level metadata (e.g. { fileCount }).
   */
  metadata?: {
    fileCount: number;
    [key: string]: unknown;
  };
  /**
   * Rule-specific result data (e.g. LedgerResult[] for Rule 37).
   * Only populated on single-run GET; null in list responses.
   */
  resultData: {
    summary?: CalculationSummary;
    findings?: AuditFindingDto[];
    ledgerResults?: LedgerResult[];
  } | null;
}

export interface AuditFindingDto {
  id?: string;
  gstin?: string;
  legalName?: string;
  severity: 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW' | 'INFO';
  reason: string;
  category?: string;
  amount: number;
  metadata?: Record<string, unknown>;
}

/** Input metadata stored with every audit run */
export interface AuditRunInputMetadata {
  asOnDate?: string;
  filename?: string;
  migratedFromV1?: boolean;
  [key: string]: unknown;
}

/** Available GST compliance rules from /api/v1/audit/rules */
export interface AuditRuleInfo {
  ruleId: string;
  name: string;
  displayName: string;
  description: string;
  category: string;
  legalBasis: string;
  creditCost: number;
}

// ─────────────────────────────────────────────
//  Pagination wrapper (generic)
// ─────────────────────────────────────────────

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
