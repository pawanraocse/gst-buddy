import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  UploadResult,
  AuditRunResponse,
  AuditRuleInfo,
  PageResponse,
} from '../../shared/models/audit.model';

const BACKEND_BASE = `${environment.apiUrl}/backend-service`;

/**
 * Audit Engine API Service.
 *
 * Replaces Rule37ApiService. Targets the generic audit engine endpoints:
 *   - POST /api/v1/ledgers/upload   (upload + execute rule)
 *   - GET  /api/v1/audit/runs       (paginated history)
 *   - GET  /api/v1/audit/runs/{id}  (single run with resultData)
 *   - DELETE /api/v1/audit/runs/{id}
 *   - GET  /api/v1/audit/runs/{id}/export  (Excel blob)
 *   - GET  /api/v1/audit/rules      (available rule catalog)
 *
 * All run IDs are UUID v7 strings.
 */
@Injectable({ providedIn: 'root' })
export class AuditApiService {
  private readonly http = inject(HttpClient);

  /**
   * Upload ledger Excel files for a given audit rule.
   * Defaults to Rule 37 (RULE_37_ITC_REVERSAL).
   */
  uploadLedgers(
    files: File[],
    asOnDate: string,
    ruleId = 'RULE_37_ITC_REVERSAL'
  ): Observable<UploadResult> {
    const formData = new FormData();
    formData.append('asOnDate', asOnDate);
    formData.append('ruleId', ruleId);
    files.forEach((f) => formData.append('files', f, f.name));
    return this.http.post<UploadResult>(`${BACKEND_BASE}/api/v1/ledgers/upload`, formData);
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
      { params }
    );
  }

  /**
   * Get a single audit run by UUID string (includes full resultData).
   */
  getRun(id: string): Observable<AuditRunResponse> {
    return this.http.get<AuditRunResponse>(`${BACKEND_BASE}/api/v1/audit/runs/${id}`);
  }

  /**
   * Delete an audit run by UUID string.
   */
  deleteRun(id: string): Observable<void> {
    return this.http.delete<void>(`${BACKEND_BASE}/api/v1/audit/runs/${id}`);
  }

  /**
   * Export an audit run to Excel (returns blob).
   * @param id      UUID v7 string of the audit run
   * @param reportType 'issues' (default), 'complete', or 'gstr3b'
   */
  exportRun(
    id: string,
    reportType: 'issues' | 'complete' | 'gstr3b' = 'issues'
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
    return this.http.get<AuditRuleInfo[]>(`${BACKEND_BASE}/api/v1/audit/rules`);
  }
}
