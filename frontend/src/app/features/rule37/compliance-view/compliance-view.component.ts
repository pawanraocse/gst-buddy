import { Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { PanelModule } from 'primeng/panel';
import { trigger, transition, style, animate } from '@angular/animations';
import { LedgerResult } from '../../../shared/models/rule37.model';

@Component({
  selector: 'app-compliance-view',
  standalone: true,
  imports: [CommonModule, ButtonModule, TableModule, PanelModule],
  templateUrl: './compliance-view.component.html',
  styleUrls: ['./compliance-view.component.scss'],
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
export class ComplianceViewComponent {
  results = input.required<LedgerResult[]>();
  runId = input<number | null>(null);
  showExportAll = input<boolean>(true);
  exportAll = output<void>();
  exportLedger = output<{ ledgerName: string; summary: LedgerResult['summary'] }>();

  // Track expanded/collapsed state per ledger
  expandedLedgers: Record<string, boolean> = {};

  get totalItcReversal(): number {
    return this.results().reduce((s, r) => s + r.summary.totalItcReversal, 0);
  }

  get totalInterest(): number {
    return this.results().reduce((s, r) => s + r.summary.totalInterest, 0);
  }

  // Risk counts for KPI cards and donut chart
  getBreachedCount(): number {
    return this.results().flatMap(r => r.summary.details)
      .filter(d => d.riskCategory === 'BREACHED').length;
  }

  getAtRiskCount(): number {
    return this.results().flatMap(r => r.summary.details)
      .filter(d => d.riskCategory === 'AT_RISK').length;
  }

  getSafeCount(): number {
    const total = this.results().flatMap(r => r.summary.details).length;
    return total - this.getBreachedCount() - this.getAtRiskCount();
  }

  // Donut chart calculations (stroke-dasharray for each segment)
  getDonutSegment(type: 'breached' | 'at-risk'): string {
    const total = Math.max(1, this.getBreachedCount() + this.getAtRiskCount() + this.getSafeCount());
    const count = type === 'breached' ? this.getBreachedCount() : this.getAtRiskCount();
    const percentage = (count / total) * 100;
    return `${percentage} ${100 - percentage}`;
  }

  getDonutOffset(type: 'at-risk'): number {
    const breachedPercent = (this.getBreachedCount() / Math.max(1, this.getBreachedCount() + this.getAtRiskCount() + this.getSafeCount())) * 100;
    return 25 - breachedPercent; // Offset after breached segment
  }

  // Toggle ledger expansion
  toggleLedger(ledgerName: string): void {
    this.expandedLedgers[ledgerName] = !this.expandedLedgers[ledgerName];
  }

  getGroupedBySupplier(lr: LedgerResult): { supplier: string; rows: typeof lr.summary.details }[] {
    const map = new Map<string, typeof lr.summary.details>();
    for (const row of lr.summary.details) {
      const list = map.get(row.supplier) ?? [];
      list.push(row);
      map.set(row.supplier, list);
    }
    return Array.from(map.entries()).map(([supplier, rows]) => ({ supplier, rows }));
  }

  formatDate(d: string | null | undefined): string {
    if (!d) return '—';
    return new Date(d).toLocaleDateString('en-IN', {
      day: '2-digit',
      month: 'short',
      year: 'numeric'
    });
  }

  formatCurrency(n: number): string {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0
    }).format(n);
  }

  getRiskClass(riskCategory: string | undefined): string {
    switch (riskCategory) {
      case 'BREACHED': return 'risk-breached';
      case 'AT_RISK': return 'risk-at-risk';
      default: return 'risk-safe';
    }
  }
}
