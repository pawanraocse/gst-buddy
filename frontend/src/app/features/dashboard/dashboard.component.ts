import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/auth.service';
import { Rule37ApiService } from '../../core/services/rule37-api.service';
import { CreditApiService, WalletDto } from '../../core/services/credit-api.service';
import { DocumentUploadComponent } from '../rule37/document-upload/document-upload.component';
import { ComplianceViewComponent } from '../rule37/compliance-view/compliance-view.component';
import { CalculationHistoryComponent } from '../rule37/calculation-history/calculation-history.component';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { InputTextModule } from 'primeng/inputtext';
import { TooltipModule } from 'primeng/tooltip';
import { LedgerResult, UploadResult } from '../../shared/models/rule37.model';
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
  ],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
})
export class DashboardComponent implements OnInit {
  authService = inject(AuthService);
  private api = inject(Rule37ApiService);
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
  runId = signal<number | null>(null);
  isProcessing = signal(false);
  error = signal<string | null>(null);
  fileNames = signal<string[]>([]);

  actionCount = computed(() => {
    const r = this.results();
    if (r.length === 0) return 0;
    const totalItc = r.reduce((s, x) => s + x.summary.totalItcReversal, 0);
    const totalInterest = r.reduce((s, x) => s + x.summary.totalInterest, 0);
    return totalItc > 0 || totalInterest > 0 ? 1 : 0;
  });

  statusMessage = computed(() => {
    const n = this.actionCount();
    if (n === 0 && this.results().length > 0) return 'You are all clear';
    if (n === 0) return '';
    if (n === 1) return '1 Action Pending';
    return `${n} Actions Pending`;
  });

  statusSeverity = computed(() => {
    const n = this.actionCount();
    if (n === 0) return 'teal';
    return 'amber';
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
    this.isProcessing.set(true);
    this.error.set(null);
    this.results.set([]);
    this.fileNames.set(files.map((f) => f.name));

    this.api.uploadLedgers(files, this.asOnDate()).subscribe({
      next: (res: UploadResult) => {
        this.runId.set(res.runId);
        this.results.set(
          res.results.map((r) => ({
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
          }))
        );
        this.fileNames.set(res.results.map((r) => r.ledgerName));
        if (res.errors.length > 0) {
          this.error.set(res.errors.map((e) => `${e.filename}: ${e.message}`).join('; '));
        } else {
          this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Files processed successfully' });
        }
        this.isProcessing.set(false);
      },
      error: (err) => {
        this.error.set(err?.message || 'Failed to process files');
        this.fileNames.set([]);
        this.isProcessing.set(false);
      },
    });
  }

  downloadExport() {
    const id = this.runId();
    const r = this.results();
    if (!id || r.length === 0) return;
    const filename = r.length === 1 ? r[0].ledgerName : `${r.length} files`;
    this.api.exportRun(id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = filename + '_Interest_Calculation.xlsx';
        a.click();
        URL.revokeObjectURL(url);
      },
      error: (err) => this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Export failed: ' + (err?.message || 'Unknown error') }),
    });
  }

  onExportLedger(ev: { ledgerName: string; summary: LedgerResult['summary'] }) {
    this.downloadExport();
  }

  switchToHistory() {
    this.activeTab.set('history');
  }
}
