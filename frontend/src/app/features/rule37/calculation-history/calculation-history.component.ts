import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';
import { Rule37ApiService } from '../../../core/services/rule37-api.service';
import {
  Rule37RunResponse,
  LedgerResult,
} from '../../../shared/models/rule37.model';
import { ComplianceViewComponent } from '../compliance-view/compliance-view.component';
import { MessageService, ConfirmationService } from 'primeng/api';
import { PaginatorModule } from 'primeng/paginator';

@Component({
  selector: 'app-calculation-history',
  standalone: true,
  imports: [CommonModule, ButtonModule, TooltipModule, ComplianceViewComponent, PaginatorModule],
  templateUrl: './calculation-history.component.html',
  styleUrls: ['./calculation-history.component.scss']
})
export class CalculationHistoryComponent {
  private api = inject(Rule37ApiService);
  private messageService = inject(MessageService);
  private confirmationService = inject(ConfirmationService);

  runs = signal<Rule37RunResponse[]>([]);
  loading = signal(false);
  error = signal<string | null>(null);
  viewingRun = signal<Rule37RunResponse | null>(null);

  currentPage = signal(0);
  pageSize = signal(10);
  totalRecords = signal(0);

  constructor() {
    this.loadRuns();
  }

  loadRuns(page: number = 0, size: number = 10) {
    this.loading.set(true);
    this.error.set(null);
    this.api.listRuns(page, size).subscribe({
      next: (pageRes) => {
        this.runs.set(pageRes.content);
        this.totalRecords.set(pageRes.totalElements || 0);
        this.currentPage.set(page);
        this.pageSize.set(size);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err?.message || 'Failed to load calculations');
        this.loading.set(false);
      },
    });
  }

  onPageChange(event: any) {
    // PrimeNG paginator passes page natively
    this.loadRuns(event.page, event.rows);
  }

  deleteRun(id: number) {
    this.confirmationService.confirm({
      message: 'Are you sure you want to delete this calculation?',
      header: 'Confirm Deletion',
      icon: 'pi pi-exclamation-triangle',
      acceptIcon: 'none',
      rejectIcon: 'none',
      acceptButtonStyleClass: 'p-button-danger',
      rejectButtonStyleClass: 'p-button-text',
      accept: () => {
        this.api.deleteRun(id).subscribe({
          next: () => {
            this.runs.update((list) => list.filter((r) => r.id !== id));
            if (this.viewingRun()?.id === id) {
              this.viewingRun.set(null);
            }
            this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Calculation deleted' });
          },
          error: (err) => {
            this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to delete: ' + (err?.message || 'Unknown error') });
          },
        });
      }
    });
  }

  downloadExport(id: number, filename: string) {
    this.api.exportRun(id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = (filename || 'export') + '_Interest_Calculation.xlsx';
        a.click();
        URL.revokeObjectURL(url);
      },
      error: (err) => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to export: ' + (err?.message || 'Unknown error') });
      },
    });
  }

  onExportLedger(
    _ev: { ledgerName: string; summary: LedgerResult['summary'] },
    runId: number,
    filename: string
  ) {
    this.downloadExport(runId, filename);
  }

  normalizeResults(data: LedgerResult[]): LedgerResult[] {
    if (!data) return [];
    return data.map((r) => ({
      ...r,
      summary: {
        ...r.summary,
        details: r.summary.details.map((d) => ({
          ...d,
          purchaseDate: typeof d.purchaseDate === 'string' ? d.purchaseDate : d.purchaseDate,
          paymentDate: d.paymentDate,
        })),
      },
    }));
  }

  formatDate(s: string): string {
    return new Date(s).toLocaleDateString('en-IN', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }
  formatCurrency(n: number): string {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 2,
    }).format(n);
  }
  getDaysRemaining(expiresAt: string): number {
    const diff = new Date(expiresAt).getTime() - Date.now();
    return Math.ceil(diff / (1000 * 60 * 60 * 24));
  }
}
