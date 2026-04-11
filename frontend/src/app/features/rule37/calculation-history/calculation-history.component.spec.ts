import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CalculationHistoryComponent } from './calculation-history.component';
import { Rule37ApiService } from '../../../core/services/rule37-api.service';
import { MessageService, ConfirmationService } from 'primeng/api';
import { of, throwError } from 'rxjs';
import { provideZonelessChangeDetection, NO_ERRORS_SCHEMA } from '@angular/core';
import { provideAnimations } from '@angular/platform-browser/animations';
import { Rule37RunResponse } from '../../../shared/models/rule37.model';

const mockRun: Rule37RunResponse = {
  id: 1,
  status: 'COMPLETED',
  filename: 'ledger_Q4',
  createdAt: '2024-03-31T10:00:00Z',
  expiresAt: '2025-03-31T10:00:00Z',
  calculationData: []
} as any;

describe('CalculationHistoryComponent', () => {
  let component: CalculationHistoryComponent;
  let fixture: ComponentFixture<CalculationHistoryComponent>;
  let apiSpy: jasmine.SpyObj<Rule37ApiService>;
  let messageServiceSpy: jasmine.SpyObj<MessageService>;
  let confirmationServiceSpy: jasmine.SpyObj<ConfirmationService>;

  beforeEach(async () => {
    const api = jasmine.createSpyObj('Rule37ApiService', ['listRuns', 'deleteRun', 'exportRun']);
    const msg = jasmine.createSpyObj('MessageService', ['add', 'messageObserver']);
    msg.messageObserver = { subscribe: () => ({}) } as any;
    const confirm = jasmine.createSpyObj('ConfirmationService', ['confirm']);

    // Return an empty page by default
    api.listRuns.and.returnValue(of({ content: [], totalElements: 0, totalPages: 0 }));

    await TestBed.configureTestingModule({
      imports: [CalculationHistoryComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideAnimations(),
        { provide: Rule37ApiService, useValue: api },
        { provide: MessageService, useValue: msg },
        { provide: ConfirmationService, useValue: confirm }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    apiSpy = TestBed.inject(Rule37ApiService) as jasmine.SpyObj<Rule37ApiService>;
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

  it('should call confirm service when deleteRun is triggered', () => {
    component.deleteRun(42);
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

    component.deleteRun(1);

    expect(apiSpy.deleteRun).toHaveBeenCalledWith(1);
    expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({
      severity: 'success'
    }));
  });

  it('downloadExport — should call exportRun with correct reportType', () => {
    const mockBlob = new Blob(['xlsx'], { type: 'application/vnd.ms-excel' });
    apiSpy.exportRun.and.returnValue(of(mockBlob));

    spyOn(URL, 'createObjectURL').and.returnValue('blob:fake');
    spyOn(URL, 'revokeObjectURL');

    component.downloadExport(1, 'ledger_Q4', 'complete');
    expect(apiSpy.exportRun).toHaveBeenCalledWith(1, 'complete');
  });

  it('countTransactions — should sum across all ledger results', () => {
    const run = {
      ...mockRun,
      calculationData: [
        { summary: { details: [{ supplier: 'A' }, { supplier: 'B' }] } },
        { summary: { details: [{ supplier: 'C' }] } }
      ]
    } as any;
    expect(component.countTransactions(run)).toBe(3);
  });

  it('formatCurrency — should format INR correctly', () => {
    const result = component.formatCurrency(1234.5);
    expect(result).toContain('₹');
    expect(result).toContain('1,234.50');
  });
});
