import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { Gstr1UploadResult } from '../models/gstr1-late-fee.model';
import { AuditApiService } from '../../../core/services/audit-api.service';

/**
 * GSTR-1 Late Fee feature API service.
 *
 * Delegates to the unified AuditApiService (POST /api/v1/audit/analyze).
 * The old /api/v1/gstr/upload endpoint has been removed.
 */
@Injectable({ providedIn: 'root' })
export class Gstr1ApiService {
  private readonly auditApi = inject(AuditApiService);

  /**
   * Upload a GSTR-1 PDF or JSON file for late fee audit.
   * Maps the generic UploadResult to Gstr1UploadResult for backward compat.
   */
  uploadGstr1(
    file: File,
    isQrmp: boolean,
    isNilReturn: boolean,
    asOnDate: string,
  ): Observable<Gstr1UploadResult> {
    return this.auditApi
      .uploadGstrDocuments([file], asOnDate, isQrmp, isNilReturn)
      .pipe(
        map((result) => <any>({
          stringRunId: result.stringRunId,
          filename: result.filename ?? file.name,
          findingsSummary: result.findingsSummary ?? [],
          creditsConsumed: result.creditsConsumed,
          remainingCredits: result.remainingCredits,
        })),
      );
  }
}
