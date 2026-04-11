import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { Rule37ApiService } from './rule37-api.service';
import { environment } from '../../../environments/environment';

describe('Rule37ApiService', () => {
  let service: Rule37ApiService;
  let httpTesting: HttpTestingController;
  const BASE = `${environment.apiUrl}/backend-service`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(),
        provideHttpClientTesting(),
        Rule37ApiService
      ]
    });

    service = TestBed.inject(Rule37ApiService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('uploadLedgers — should POST to /api/v1/ledgers/upload with FormData', () => {
    const file = new File(['data'], 'ledger.xlsx', { type: 'application/vnd.ms-excel' });
    service.uploadLedgers([file], '2024-03-31').subscribe();

    const req = httpTesting.expectOne(`${BASE}/api/v1/ledgers/upload`);
    expect(req.request.method).toBe('POST');
    // body is a FormData — verify filename and date are attached
    const body = req.request.body as FormData;
    expect(body.get('asOnDate')).toBe('2024-03-31');
    expect(body.has('files')).toBe(true);
    req.flush({ runId: 42, status: 'PENDING' });
  });

  it('listRuns — should GET /api/v1/rule37/runs with pagination params', () => {
    service.listRuns(0, 10).subscribe();

    const req = httpTesting.expectOne(r =>
      r.url === `${BASE}/api/v1/rule37/runs` &&
      r.params.get('page') === '0' &&
      r.params.get('size') === '10' &&
      r.params.get('sort') === 'createdAt,desc'
    );
    expect(req.request.method).toBe('GET');
    req.flush({ content: [], totalElements: 0, totalPages: 0 });
  });

  it('getRun — should GET /api/v1/rule37/runs/{id}', () => {
    service.getRun(1).subscribe(run => {
      expect(run.id).toBe(1);
    });

    const req = httpTesting.expectOne(`${BASE}/api/v1/rule37/runs/1`);
    expect(req.request.method).toBe('GET');
    req.flush({ id: 1, status: 'COMPLETED' });
  });

  it('deleteRun — should DELETE /api/v1/rule37/runs/{id}', () => {
    service.deleteRun(5).subscribe();

    const req = httpTesting.expectOne(`${BASE}/api/v1/rule37/runs/5`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('exportRun — should GET /api/v1/rule37/runs/{id}/export with reportType param', () => {
    service.exportRun(3, 'complete').subscribe();

    const req = httpTesting.expectOne(r =>
      r.url === `${BASE}/api/v1/rule37/runs/3/export` &&
      r.params.get('reportType') === 'complete'
    );
    expect(req.request.method).toBe('GET');
    req.flush(new Blob(['xlsx'], { type: 'application/vnd.ms-excel' }));
  });

  it('exportRun — defaults reportType to issues', () => {
    service.exportRun(7).subscribe();

    const req = httpTesting.expectOne(r =>
      r.url === `${BASE}/api/v1/rule37/runs/7/export` &&
      r.params.get('reportType') === 'issues'
    );
    expect(req.request.method).toBe('GET');
    req.flush(new Blob());
  });
});
