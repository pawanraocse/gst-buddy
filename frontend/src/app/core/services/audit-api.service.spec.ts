import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { AuditApiService } from './audit-api.service';
import { environment } from '../../../environments/environment';

describe('AuditApiService', () => {
  let service: AuditApiService;
  let httpTesting: HttpTestingController;
  const BASE = `${environment.apiUrl}/backend-service`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(),
        provideHttpClientTesting(),
        AuditApiService
      ]
    });

    service = TestBed.inject(AuditApiService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  // ─── Upload ────────────────────────────────────────────

  it('uploadLedgers — should POST to /api/v1/ledgers/upload with ruleId', () => {
    const file = new File(['data'], 'ledger.xlsx', { type: 'application/vnd.ms-excel' });
    service.uploadLedgers([file], '2024-03-31').subscribe();

    const req = httpTesting.expectOne(`${BASE}/api/v1/ledgers/upload`);
    expect(req.request.method).toBe('POST');
    const body = req.request.body as FormData;
    expect(body.get('asOnDate')).toBe('2024-03-31');
    expect(body.get('ruleId')).toBe('RULE_37_ITC_REVERSAL');  // default
    expect(body.has('files')).toBe(true);
    req.flush({ stringRunId: '018e6e9e-0000-7abc-8def-000000000042', results: [], errors: [] });
  });

  it('uploadLedgers — should pass custom ruleId', () => {
    const file = new File(['data'], 'ledger.xlsx');
    service.uploadLedgers([file], '2024-03-31', 'LATE_FEE_GSTR1').subscribe();

    const req = httpTesting.expectOne(`${BASE}/api/v1/ledgers/upload`);
    const body = req.request.body as FormData;
    expect(body.get('ruleId')).toBe('LATE_FEE_GSTR1');
    req.flush({ stringRunId: '018e6e9e-1111-7abc-8def-000000000001', results: [], errors: [] });
  });

  // ─── List Runs ─────────────────────────────────────────

  it('listRuns — should GET /api/v1/audit/runs with pagination params', () => {
    service.listRuns(0, 10).subscribe();

    const req = httpTesting.expectOne(r =>
      r.url === `${BASE}/api/v1/audit/runs` &&
      r.params.get('page') === '0' &&
      r.params.get('size') === '10' &&
      r.params.get('sort') === 'createdAt,desc'
    );
    expect(req.request.method).toBe('GET');
    req.flush({ content: [], totalElements: 0, totalPages: 0 });
  });

  it('listRuns — should support custom page and size', () => {
    service.listRuns(2, 5).subscribe();

    const req = httpTesting.expectOne(r =>
      r.url === `${BASE}/api/v1/audit/runs` &&
      r.params.get('page') === '2' &&
      r.params.get('size') === '5'
    );
    expect(req.request.method).toBe('GET');
    req.flush({ content: [], totalElements: 0, totalPages: 0 });
  });

  // ─── Get Run (UUID string ID) ───────────────────────────

  it('getRun — should GET /api/v1/audit/runs/{uuid}', () => {
    const uuid = '018e6e9e-0000-7abc-8def-000000000042';
    service.getRun(uuid).subscribe(run => {
      expect(run.id).toBe(uuid);
    });

    const req = httpTesting.expectOne(`${BASE}/api/v1/audit/runs/${uuid}`);
    expect(req.request.method).toBe('GET');
    req.flush({ id: uuid, status: 'SUCCESS', ruleId: 'RULE_37_ITC_REVERSAL' });
  });

  // ─── Delete Run (UUID string ID) ────────────────────────

  it('deleteRun — should DELETE /api/v1/audit/runs/{uuid}', () => {
    const uuid = '018e6e9e-0000-7abc-8def-000000000099';
    service.deleteRun(uuid).subscribe();

    const req = httpTesting.expectOne(`${BASE}/api/v1/audit/runs/${uuid}`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  // ─── Export Run (UUID string ID + reportType) ───────────

  it('exportRun — should GET /api/v1/audit/runs/{uuid}/export with reportType', () => {
    const uuid = '018e6e9e-0000-7abc-8def-000000000003';
    service.exportRun(uuid, 'complete').subscribe();

    const req = httpTesting.expectOne(r =>
      r.url === `${BASE}/api/v1/audit/runs/${uuid}/export` &&
      r.params.get('reportType') === 'complete'
    );
    expect(req.request.method).toBe('GET');
    req.flush(new Blob(['xlsx'], { type: 'application/vnd.ms-excel' }));
  });

  it('exportRun — should default reportType to issues', () => {
    const uuid = '018e6e9e-0000-7abc-8def-000000000007';
    service.exportRun(uuid).subscribe();

    const req = httpTesting.expectOne(r =>
      r.url === `${BASE}/api/v1/audit/runs/${uuid}/export` &&
      r.params.get('reportType') === 'issues'
    );
    expect(req.request.method).toBe('GET');
    req.flush(new Blob());
  });

  it('exportRun — should support gstr3b reportType', () => {
    const uuid = '018e6e9e-0000-7abc-8def-000000000008';
    service.exportRun(uuid, 'gstr3b').subscribe();

    const req = httpTesting.expectOne(r =>
      r.url === `${BASE}/api/v1/audit/runs/${uuid}/export` &&
      r.params.get('reportType') === 'gstr3b'
    );
    expect(req.request.method).toBe('GET');
    req.flush(new Blob());
  });

  // ─── Available Rules Catalog ────────────────────────────

  it('getAvailableRules — should GET /api/v1/audit/rules', () => {
    service.getAvailableRules().subscribe(rules => {
      expect(rules.length).toBe(1);
      expect(rules[0].ruleId).toBe('RULE_37_ITC_REVERSAL');
    });

    const req = httpTesting.expectOne(`${BASE}/api/v1/audit/rules`);
    expect(req.request.method).toBe('GET');
    req.flush([{ ruleId: 'RULE_37_ITC_REVERSAL', displayName: 'Rule 37 — 180-Day ITC Reversal', legalBasis: 'Section 16(2)', creditsRequired: 1 }]);
  });
});
