import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  UploadResult,
  AuditRunResponse,
  AuditRuleInfo,
  PageResponse,
  AnalysisMode,
} from '../../shared/models/audit.model';

const BACKEND_BASE = `${environment.apiUrl}/backend-service`;

/**
 * Audit Engine API Service.
 *
 * All analysis goes through the unified POST /api/v1/audit/analyze endpoint.
 *
 * Endpoints:
 *   - POST /api/v1/audit/analyze     (unified document-centric analysis)
 *   - GET  /api/v1/audit/runs        (paginated history)
 *   - GET  /api/v1/audit/runs/{id}   (single run with resultData)
 *   - DELETE /api/v1/audit/runs/{id}
 *   - GET  /api/v1/audit/runs/{id}/export (Excel blob)
 *   - GET  /api/v1/audit/rules       (available rule catalog)
 *
 * Credits: 20 for GSTR_RULES_ANALYSIS, 1 for LEDGER_ANALYSIS.
 */
@Injectable({ providedIn: 'root' })
export class AuditApiService {
  private readonly http = inject(HttpClient);

  /**
   * Run a comprehensive GST compliance audit.
   *
   * @param files           one or more document files (Excel, PDF, JSON)
   * @param analysisMode    LEDGER_ANALYSIS | GSTR_RULES_ANALYSIS
   * @param asOnDate        ISO date string, e.g. '2025-03-31'
   * @param isQrmp          true if taxpayer is a QRMP filer (default false)
   * @param isNilReturn     true if filing is a nil-return (default false)
   * @param aggregateTurnover  annual turnover in INR (optional)
   */
  analyze(
    files: File[],
    analysisMode: AnalysisMode,
    asOnDate: string,
    isQrmp = false,
    isNilReturn = false,
    aggregateTurnover?: number,
  ): Observable<UploadResult> {
    const formData = new FormData();
    formData.append('analysisMode', analysisMode);
    formData.append('asOnDate', asOnDate);
    formData.append('isQrmp', String(isQrmp));
    formData.append('isNilReturn', String(isNilReturn));
    if (aggregateTurnover != null) {
      formData.append('aggregateTurnover', String(aggregateTurnover));
    }
    files.forEach((f) => formData.append('files', f, f.name));
    return this.http.post<UploadResult>(
      `${BACKEND_BASE}/api/v1/audit/analyze`,
      formData,
    );
  }

  /**
   * Convenience wrapper: upload ledger Excel files (LEDGER_ANALYSIS mode).
   * 1 credit per run.
   */
  uploadLedgers(files: File[], asOnDate: string): Observable<UploadResult> {
    return this.analyze(files, 'LEDGER_ANALYSIS', asOnDate);
  }

  /**
   * Convenience wrapper: upload GSTR documents (GSTR_RULES_ANALYSIS mode).
   * 20 credits flat per run.
   */
  uploadGstrDocuments(
    files: File[],
    asOnDate: string,
    isQrmp = false,
    isNilReturn = false,
    aggregateTurnover?: number,
  ): Observable<UploadResult> {
    return this.analyze(files, 'GSTR_RULES_ANALYSIS', asOnDate, isQrmp, isNilReturn, aggregateTurnover);
  }

  /**
   * List audit runs (paginated, sorted by createdAt desc).
   */
  listRuns(page = 0, size = 10): Observable<PageResponse<AuditRunResponse>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', 'createdAt,desc');
    return this.http.get<PageResponse<AuditRunResponse>>(
      `${BACKEND_BASE}/api/v1/audit/runs`,
      { params },
    );
  }

  /**
   * Get a single audit run by UUID string (includes full resultData).
   */
  getRun(id: string): Observable<AuditRunResponse> {
    return this.http.get<AuditRunResponse>(
      `${BACKEND_BASE}/api/v1/audit/runs/${id}`,
    );
  }

  /**
   * Delete an audit run by UUID string.
   */
  deleteRun(id: string): Observable<void> {
    return this.http.delete<void>(`${BACKEND_BASE}/api/v1/audit/runs/${id}`);
  }

  /**
   * Export an audit run to Excel (returns blob).
   * @param id         UUID v7 string of the audit run
   * @param reportType 'issues' (default), 'complete', or 'gstr3b'
   */
  exportRun(
    id: string,
    reportType: 'issues' | 'complete' | 'gstr3b' = 'issues',
  ): Observable<Blob> {
    const params = new HttpParams().set('reportType', reportType);
    return this.http.get(`${BACKEND_BASE}/api/v1/audit/runs/${id}/export`, {
      responseType: 'blob',
      params,
    });
  }

  /**
   * Fetch the catalog of available GST audit rules from the backend registry.
   */
  getAvailableRules(): Observable<AuditRuleInfo[]> {
    return this.http.get<AuditRuleInfo[]>(
      `${BACKEND_BASE}/api/v1/audit/rules`,
    );
  }
}
