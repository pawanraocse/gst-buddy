/**
 * GSTR-1 Late Fee feature models.
 * Mirrors backend UploadResult.FindingSummaryDto and related records.
 */

export type FindingSeverity = 'HIGH' | 'MEDIUM' | 'LOW' | 'INFO';

export interface FindingSummaryDto {
  ruleId: string;
  severity: FindingSeverity;
  legalBasis: string;
  compliancePeriod: string;
  impactAmount: number;
  description: string;
  recommendedAction: string;
}

/** Response from POST /api/v1/gstr/upload */
export interface Gstr1UploadResult {
  stringRunId: string;
  filename: string;
  findingsSummary: FindingSummaryDto[];
  creditsConsumed: number;
  remainingCredits: number;
}

/** Upload form values */
export interface Gstr1UploadForm {
  isQrmp: boolean;
  isNilReturn: boolean;
  asOnDate: Date;
}
