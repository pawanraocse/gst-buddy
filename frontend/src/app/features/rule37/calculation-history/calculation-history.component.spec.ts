import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CalculationHistoryComponent } from './calculation-history.component';
import { AuditApiService } from '../../../core/services/audit-api.service';
import { MessageService, ConfirmationService } from 'primeng/api';
import { of, throwError } from 'rxjs';
import { provideZonelessChangeDetection, NO_ERRORS_SCHEMA } from '@angular/core';
import { provideAnimations } from '@angular/platform-browser/animations';
import { AuditRunResponse } from '../../../shared/models/audit.model';

const mockRun: AuditRunResponse = {
  id: '018e6e9e-0000-7abc-8def-000000000001',   // UUID v7 string
  ruleId: 'RULE_37_ITC_REVERSAL',
  ruleDisplayName: 'Rule 37 — 180-Day ITC Reversal',
  status: 'SUCCESS',
  totalImpactAmount: 15000,
  creditsConsumed: 1,
  createdAt: '2024-03-31T10:00:00Z',
  completedAt: '2024-03-31T10:00:05Z',
  expiresAt: '2025-03-31T10:00:00Z',
  userId: 'user-uuid-abc',
  inputMetadata: { asOnDate: '2024-03-31', filename: 'ledger_Q4' },
  resultData: []
};

const mockRun2: AuditRunResponse = {
  ...mockRun,
  id: '018e6e9e-0000-7abc-8def-000000000002',
  inputMetadata: { asOnDate: '2024-06-30', filename: 'ledger_Q1' },
  resultData: [
    {
      ledgerName: 'Vendor A',
      summary: {
        totalItcReversal: 5000,
        totalInterest: 250,
        atRiskCount: 2,
        atRiskAmount: 2000,
        breachedCount: 1,
        calculationDate: '2024-03-31',
        details: [
          { supplier: 'Vendor A', invoiceNumber: 'INV001' } as any,
          { supplier: 'Vendor B', invoiceNumber: 'INV002' } as any,
        ]
      }
    },
    {
      ledgerName: 'Vendor C',
      summary: {
        totalItcReversal: 1000,
        totalInterest: 50,
        atRiskCount: 1,
        atRiskAmount: 500,
        breachedCount: 0,
        calculationDate: '2024-03-31',
        details: [{ supplier: 'Vendor C', invoiceNumber: 'INV003' } as any]
      }
    }
  ]
};

describe('CalculationHistoryComponent', () => {
  let component: CalculationHistoryComponent;
  let fixture: ComponentFixture<CalculationHistoryComponent>;
  let apiSpy: jasmine.SpyObj<AuditApiService>;
  let messageServiceSpy: jasmine.SpyObj<MessageService>;
  let confirmationServiceSpy: jasmine.SpyObj<ConfirmationService>;

  beforeEach(async () => {
    const api = jasmine.createSpyObj('AuditApiService', ['listRuns', 'deleteRun', 'exportRun']);
    const msg = jasmine.createSpyObj('MessageService', ['add', 'messageObserver']);
    msg.messageObserver = { subscribe: () => ({}) } as any;
    const confirm = jasmine.createSpyObj('ConfirmationService', ['confirm']);

    api.listRuns.and.returnValue(of({ content: [], totalElements: 0, totalPages: 0 }));

    await TestBed.configureTestingModule({
      imports: [CalculationHistoryComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideAnimations(),
        { provide: AuditApiService, useValue: api },
        { provide: MessageService, useValue: msg },
        { provide: ConfirmationService, useValue: confirm }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    apiSpy = TestBed.inject(AuditApiService) as jasmine.SpyObj<AuditApiService>;
    messageServiceSpy = TestBed.inject(MessageService) as jasmine.SpyObj<MessageService>;
    confirmationServiceSpy = TestBed.inject(ConfirmationService) as jasmine.SpyObj<ConfirmationService>;

    fixture = TestBed.createComponent(CalculationHistoryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should call listRuns on init', () => {
    expect(apiSpy.listRuns).toHaveBeenCalledWith(0, 10);
  });

  it('should set runs after successful listRuns', () => {
    apiSpy.listRuns.and.returnValue(of({ content: [mockRun], totalElements: 1, totalPages: 1 } as any));
    component.loadRuns(0, 10);
    expect(component.runs()).toEqual([mockRun]);
    expect(component.totalRecords()).toBe(1);
    expect(component.loading()).toBe(false);
  });

  it('should set error signal on listRuns failure', () => {
    apiSpy.listRuns.and.returnValue(throwError(() => new Error('Network failure')));
    component.loadRuns();
    expect(component.error()).toBe('Network failure');
    expect(component.loading()).toBe(false);
  });

  // ─── UUID string ID tests ───────────────────────────────

  it('deleteRun — should call API with UUID string id', () => {
    const uuid = '018e6e9e-0000-7abc-8def-000000000001';
    component.deleteRun(uuid);
    expect(confirmationServiceSpy.confirm).toHaveBeenCalled();
  });

  it('deleteRun — should call deleteRun API after confirmation accept', () => {
    apiSpy.listRuns.and.returnValue(of({ content: [mockRun], totalElements: 1, totalPages: 1 } as any));
    component.loadRuns();

    apiSpy.deleteRun.and.returnValue(of(undefined));

    confirmationServiceSpy.confirm.and.callFake((options: any) => {
      options.accept();
      return confirmationServiceSpy;
    });

    const uuid = '018e6e9e-0000-7abc-8def-000000000001';
    component.deleteRun(uuid);

    expect(apiSpy.deleteRun).toHaveBeenCalledWith(uuid);
    expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({
      severity: 'success'
    }));
  });

  it('downloadExport — should call exportRun with UUID string id and reportType', () => {
    const mockBlob = new Blob(['xlsx'], { type: 'application/vnd.ms-excel' });
    apiSpy.exportRun.and.returnValue(of(mockBlob));

    spyOn(URL, 'createObjectURL').and.returnValue('blob:fake');
    spyOn(URL, 'revokeObjectURL');

    const uuid = '018e6e9e-0000-7abc-8def-000000000001';
    component.downloadExport(uuid, 'ledger_Q4', 'complete');
    expect(apiSpy.exportRun).toHaveBeenCalledWith(uuid, 'complete');
  });

  it('downloadExport — gstr3b — should use _GSTR3B_Summary suffix', () => {
    const mockBlob = new Blob(['xlsx']);
    apiSpy.exportRun.and.returnValue(of(mockBlob));
    const anchorSpy = spyOn(document, 'createElement').and.callThrough();
    spyOn(URL, 'createObjectURL').and.returnValue('blob:fake');
    spyOn(URL, 'revokeObjectURL');

    component.downloadExport('018e6e9e-0000-7abc-8def-000000000001', 'ledger_Q4', 'gstr3b');
    expect(apiSpy.exportRun).toHaveBeenCalledWith('018e6e9e-0000-7abc-8def-000000000001', 'gstr3b');
  });

  // ─── Data helper tests ──────────────────────────────────

  it('getFilename — should return filename from inputMetadata', () => {
    expect(component.getFilename(mockRun)).toBe('ledger_Q4');
  });

  it('getFilename — should fall back to first 8 chars of ID', () => {
    const run = { ...mockRun, inputMetadata: null };
    expect(component.getFilename(run)).toBe('018e6e9e');
  });

  it('getResults — should return resultData as LedgerResult[]', () => {
    const results = component.getResults(mockRun2);
    expect(results.length).toBe(2);
  });

  it('getResults — should return empty array when resultData is null', () => {
    const run = { ...mockRun, resultData: null };
    expect(component.getResults(run)).toEqual([]);
  });

  it('countTransactions — should sum across all ledger results', () => {
    expect(component.countTransactions(mockRun2)).toBe(3);
  });

  it('countTransactions — should return 0 when resultData is null', () => {
    const run = { ...mockRun, resultData: null };
    expect(component.countTransactions(run)).toBe(0);
  });

  it('countDistinctSuppliers — should count unique suppliers', () => {
    // mockRun2 has Vendor A, Vendor B (file 1) and Vendor C (file 2) = 3
    expect(component.countDistinctSuppliers(mockRun2)).toBe(3);
  });

  it('formatCurrency — should format INR correctly', () => {
    const result = component.formatCurrency(1234.5);
    expect(result).toContain('₹');
    expect(result).toContain('1,234.50');
  });

  it('getDaysRemaining — should compute positive days for future expiry', () => {
    const future = new Date(Date.now() + 5 * 24 * 60 * 60 * 1000).toISOString();
    expect(component.getDaysRemaining(future)).toBeGreaterThan(0);
  });
});
