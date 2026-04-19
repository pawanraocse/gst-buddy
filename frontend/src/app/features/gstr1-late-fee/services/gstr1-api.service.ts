import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Gstr1UploadResult } from '../models/gstr1-late-fee.model';
import { environment } from '../../../../environments/environment';

/**
 * HTTP service for the GSTR-1 Late Fee feature.
 * Base URL: POST /api/v1/gstr/upload
 *
 * Auth header is injected automatically by AuthInterceptor (Amplify JWT).
 * Error handling is delegated to the global ErrorInterceptor.
 */
@Injectable({ providedIn: 'root' })
export class Gstr1ApiService {
  private readonly http = inject(HttpClient);
  private readonly BASE = `${environment.apiUrl}/api/v1/gstr`;

  /**
   * Upload a GSTR-1 PDF or JSON file for late fee audit.
   *
   * @param file        GSTR-1 PDF or JSON file
   * @param isQrmp      true if taxpayer is a QRMP filer
   * @param isNilReturn true if this is a nil-return filing
   * @param asOnDate    compliance evaluation date (ISO string)
   */
  uploadGstr1(
    file: File,
    isQrmp: boolean,
    isNilReturn: boolean,
    asOnDate: string
  ): Observable<Gstr1UploadResult> {
    const form = new FormData();
    form.append('file', file);
    form.append('isQrmp', String(isQrmp));
    form.append('isNilReturn', String(isNilReturn));
    form.append('asOnDate', asOnDate);
    return this.http.post<Gstr1UploadResult>(`${this.BASE}/upload`, form);
  }
}
