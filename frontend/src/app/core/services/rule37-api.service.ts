import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  UploadResult,
  Rule37RunResponse,
  PageResponse,
} from '../../shared/models/rule37.model';

const BACKEND_BASE = `${environment.apiUrl}/backend-service`;

/**
 * Rule 37 API Service - Ledger upload, runs, and export.
 */
@Injectable({ providedIn: 'root' })
export class Rule37ApiService {
  private readonly http = inject(HttpClient);

  /**
   * Upload ledger Excel files for Rule 37 calculation.
   */
  uploadLedgers(files: File[], asOnDate: string): Observable<UploadResult> {
    const formData = new FormData();
    formData.append('asOnDate', asOnDate);
    files.forEach((f) => formData.append('files', f, f.name));
    return this.http.post<UploadResult>(`${BACKEND_BASE}/api/v1/ledgers/upload`, formData);
  }

  /**
   * List calculation runs (paginated).
   */
  listRuns(page = 0, size = 10): Observable<PageResponse<Rule37RunResponse>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', 'createdAt,desc');
    return this.http.get<PageResponse<Rule37RunResponse>>(
      `${BACKEND_BASE}/api/v1/rule37/runs`,
      { params }
    );
  }

  /**
   * Get a single run by ID (includes full calculation data).
   */
  getRun(id: number): Observable<Rule37RunResponse> {
    return this.http.get<Rule37RunResponse>(`${BACKEND_BASE}/api/v1/rule37/runs/${id}`);
  }

  /**
   * Delete a run.
   */
  deleteRun(id: number): Observable<void> {
    return this.http.delete<void>(`${BACKEND_BASE}/api/v1/rule37/runs/${id}`);
  }

  /**
   * Export run to Excel (returns blob).
   * @param reportType 'issues' (default), 'complete', or 'gstr3b'
   */
  exportRun(id: number, reportType: 'issues' | 'complete' | 'gstr3b' = 'issues'): Observable<Blob> {
    const params = new HttpParams().set('reportType', reportType);
    return this.http.get(`${BACKEND_BASE}/api/v1/rule37/runs/${id}/export`, {
      responseType: 'blob',
      params,
    });
  }
}
