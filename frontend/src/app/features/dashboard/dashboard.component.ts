import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { trigger, transition, style, animate } from '@angular/animations';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/auth.service';
import { AuditApiService } from '../../core/services/audit-api.service';
import { CreditApiService, WalletDto } from '../../core/services/credit-api.service';
import { DocumentUploadComponent } from '../rule37/document-upload/document-upload.component';
import { ComplianceViewComponent } from '../rule37/compliance-view/compliance-view.component';
import { CalculationHistoryComponent } from '../rule37/calculation-history/calculation-history.component';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { InputTextModule } from 'primeng/inputtext';
import { TooltipModule } from 'primeng/tooltip';
import { PanelModule } from 'primeng/panel';
import { LedgerResult, UploadResult, AuditRuleInfo, AnalysisMode, FindingSummary } from '../../shared/models/audit.model';
import { Select } from 'primeng/select';
import { DatePicker } from 'primeng/datepicker';
import { MessageService } from 'primeng/api';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    CardModule,
    ButtonModule,
    MessageModule,
    InputTextModule,
    TooltipModule,
    DocumentUploadComponent,
    ComplianceViewComponent,
    CalculationHistoryComponent,
    PanelModule,
    Select,
    DatePicker
  ],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
  animations: [
    trigger('slideDown', [
      transition(':enter', [
        style({ opacity: 0, height: 0, overflow: 'hidden' }),
        animate('300ms ease-out', style({ opacity: 1, height: '*' }))
      ]),
      transition(':leave', [
        animate('200ms ease-in', style({ opacity: 0, height: 0 }))
      ])
    ])
  ]
})
export class DashboardComponent implements OnInit {
  authService = inject(AuthService);
  private api = inject(AuditApiService);
  private creditApi = inject(CreditApiService);
  private messageService = inject(MessageService);

  // Credit wallet
  creditTotal = signal(0);
  creditUsed = signal(0);
  creditRemaining = signal(0);
  walletLoaded = signal(false);

  activeTab = signal<'new' | 'history'>('new');
  asOnDate = signal<string>(new Date().toISOString().split('T')[0]);
  results = signal<LedgerResult[]>([]);
  runId = signal<string | null>(null);
  isProcessing = signal(false);
  error = signal<string | null>(null);
  fileNames = signal<string[]>([]);
  scopeConfirmed = signal(false);

  analysisMode = signal<AnalysisMode>('LEDGER_ANALYSIS');
  isQrmp = signal(false);
  isNilReturn = signal(false);
  aggregateTurnover = signal<number | null>(null);

  gstrFindings = signal<FindingSummary[]>([]);



  // At-risk KPIs
  atRiskCount = computed(() => {
    return this.results().reduce((s, x) => s + (x.summary.atRiskCount ?? 0), 0);
  });

  atRiskAmount = computed(() => {
    return this.results().reduce((s, x) => s + (x.summary.atRiskAmount ?? 0), 0);
  });

  /**
   * 3-state banner: 'clear' | 'watchlist' | 'action'
   * - action: breached invoices exist (ITC reversal or interest > 0)
   * - watchlist: no breaches but at-risk invoices (150–180 days)
   * - clear: no issues at all
   */
  bannerState = computed<'clear' | 'watchlist' | 'action'>(() => {
    if (this.analysisMode() === 'GSTR_RULES_ANALYSIS') {
      const gstr = this.gstrFindings();
      if (gstr.length === 0) return 'clear';
      if (gstr.some(f => f.severity === 'CRITICAL' || f.severity === 'HIGH')) return 'action';
      if (gstr.some(f => f.severity === 'MEDIUM' || f.severity === 'LOW')) return 'watchlist';
      return 'clear';
    } else {
      const r = this.results();
      if (r.length === 0) return 'clear';
      const totalItc = r.reduce((s, x) => s + x.summary.totalItcReversal, 0);
      const totalInterest = r.reduce((s, x) => s + x.summary.totalInterest, 0);
      if (totalItc > 0 || totalInterest > 0) return 'action';
      if (this.atRiskCount() > 0) return 'watchlist';
      return 'clear';
    }
  });

  statusMessage = computed(() => {
    const state = this.bannerState();
    if (this.analysisMode() === 'LEDGER_ANALYSIS') {
      if (this.results().length === 0) return '';
      if (state === 'clear') return 'All Clear — No estimated liability';
      if (state === 'watchlist') return `Watchlist — ${this.atRiskCount()} invoice(s) due soon`;
      return 'Action Needed';
    } else {
      if (this.fileNames().length === 0) return '';
      if (state === 'clear') return 'All Clear — No compliance issues detected';
      if (state === 'watchlist') return 'Attention — Non-critical issues or manual checks needed';
      return 'Action Needed — High severity issues detected';
    }
  });

  statusSeverity = computed(() => {
    const state = this.bannerState();
    if (state === 'action') return 'red';
    if (state === 'watchlist') return 'amber';
    return 'teal';
  });

  // KPI Summary computed values
  totalItcReversal = computed(() => {
    return this.results().reduce((s, x) => s + x.summary.totalItcReversal, 0);
  });

  totalInterest = computed(() => {
    return this.results().reduce((s, x) => s + x.summary.totalInterest, 0);
  });

  filesProcessed = computed(() => this.results().length);

  formatCurrency(value: number): string {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(value);
  }

  ngOnInit(): void {
    this.loadWallet();
  }

  private loadWallet(): void {
    this.creditApi.getWallet().subscribe({
      next: (w) => {
        this.creditTotal.set(w.total);
        this.creditUsed.set(w.used);
        this.creditRemaining.set(w.remaining);
        this.walletLoaded.set(true);
      },
      error: () => { /* wallet unavailable, keep defaults */ }
    });
  }

  onFilesSelected(files: File[]) {
    // Rules check not strictly necessary for ledger mode as the orchestrator defaults to Rule 37.
    this.isProcessing.set(true);
    this.error.set(null);
    this.results.set([]);
    this.gstrFindings.set([]);
    this.fileNames.set(files.map((f) => f.name));

    const obs$ = this.analysisMode() === 'LEDGER_ANALYSIS' 
      ? this.api.uploadLedgers(files, this.asOnDate())
      : this.api.uploadGstrDocuments(files, this.asOnDate(), this.isQrmp(), this.isNilReturn(), this.aggregateTurnover() ?? undefined);

    obs$.subscribe({
      next: (res: UploadResult) => {
        this.runId.set(res.stringRunId);
        
        if (this.analysisMode() === 'LEDGER_ANALYSIS') {
          this.results.set(
            res.results?.map((r) => ({
              ledgerName: r.ledgerName,
              summary: {
                totalInterest: r.summary.totalInterest,
                totalItcReversal: r.summary.totalItcReversal,
                atRiskCount: r.summary.atRiskCount ?? 0,
                atRiskAmount: r.summary.atRiskAmount ?? 0,
                breachedCount: r.summary.breachedCount ?? 0,
                calculationDate: r.summary.calculationDate ?? new Date().toISOString().split('T')[0],
                details: r.summary.details.map((d) => ({
                  ...d,
                  purchaseDate: d.purchaseDate ?? '',
                  paymentDate: d.paymentDate ?? null,
                })),
              },
            })) ?? []
          );
        } else {
          this.gstrFindings.set(res.findingsSummary ?? []);
        }

        this.fileNames.set(files.map((f) => f.name));
        
        if (res.errors && res.errors.length > 0) {
          this.error.set(res.errors.map((e) => `${e.filename}: ${e.message}`).join('; '));
        } else {
          this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Files processed successfully' });
        }
        this.isProcessing.set(false);
      },
      error: (err) => {
        if (err?.status === 402) {
          const body = err?.error;
          const detail = body?.message || body?.error || 'You do not have enough credits to process these files. Please purchase more credits.';
          this.error.set(detail);
          this.messageService.add({
            severity: 'warn',
            summary: 'Insufficient Credits',
            detail,
            life: 8000,
          });
          this.loadWallet();
        } else {
          const detail = err?.error?.message || err?.message || 'Failed to process files';
          this.error.set(detail);
          this.messageService.add({
            severity: 'error',
            summary: 'Upload Failed',
            detail,
          });
        }
        this.fileNames.set([]);
        this.isProcessing.set(false);
      },
    });
  }

  downloadExport(reportType: string = 'issues') {
    const rt = (['issues', 'complete', 'gstr3b'].includes(reportType) ? reportType : 'issues') as 'issues' | 'complete' | 'gstr3b';
    const id = this.runId();
    const r = this.results();
    if (!id || r.length === 0) return;
    const filename = r.length === 1 ? r[0].ledgerName : `${r.length} files`;
    const suffix = rt === 'gstr3b' ? '_GSTR3B_Summary' : '_Interest_Calculation';
    this.api.exportRun(id, rt).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = filename + suffix + '.xlsx';
        a.click();
        URL.revokeObjectURL(url);
      },
      error: (err) => this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Export failed: ' + (err?.message || 'Unknown error') }),
    });
  }

  onExportLedger(ev: { ledgerName: string; summary: LedgerResult['summary'] }) {
    this.downloadExport('issues');
  }

  switchToHistory() {
    this.activeTab.set('history');
  }
}
