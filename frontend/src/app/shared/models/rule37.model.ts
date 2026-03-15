/**
 * Rule 37 (180-day ITC reversal) models.
 * Maps to backend DTOs.
 */

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
  // Production enhancements
  paymentDeadline: string;         // purchaseDate + 180 days
  riskCategory: RiskCategory;      // SAFE, AT_RISK, BREACHED
  gstr3bPeriod: string;            // Return period for reversal (e.g., "Jan 2025")
  daysToDeadline: number;          // Days until/since 180-day deadline (negative = breached)
  itcAvailmentDate: string | null; // Date ITC was likely availed (20th of next month)
}

export interface CalculationSummary {
  totalInterest: number;
  totalItcReversal: number;
  details: InterestRow[];
  // Production enhancements
  atRiskCount: number;
  atRiskAmount: number;
  breachedCount: number;
  calculationDate: string;
}

export interface LedgerResult {
  ledgerName: string;
  summary: CalculationSummary;
}

export interface UploadResult {
  runId: number;
  filename: string;
  results: { ledgerName: string; summary: CalculationSummary }[];
  errors: { filename: string; message: string }[];
}

export interface Rule37RunResponse {
  id: number;
  filename: string;
  asOnDate: string;
  totalInterest: number | null;
  totalItcReversal: number | null;
  createdAt: string;
  createdBy: string | null;
  expiresAt: string;
  calculationData: LedgerResult[];
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
