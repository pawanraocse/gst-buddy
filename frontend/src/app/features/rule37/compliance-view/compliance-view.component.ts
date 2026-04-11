import { Component, computed, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { PanelModule } from 'primeng/panel';
import { MenuModule } from 'primeng/menu';
import { TooltipModule } from 'primeng/tooltip';
import { trigger, transition, style, animate } from '@angular/animations';
import { LedgerResult, InterestRow } from '../../../shared/models/audit.model';
import { MenuItem } from 'primeng/api';

type StatusFilter = 'ALL' | 'UNPAID' | 'PAID_LATE' | 'PAID_ON_TIME';
type RiskFilter = 'ALL' | 'BREACHED' | 'AT_RISK' | 'SAFE';
type SortOption = 'risk' | 'amount-desc' | 'amount-asc' | 'date-desc' | 'date-asc';

interface SupplierGroup {
  supplier: string;
  rows: InterestRow[];
  totalItc: number;
  totalInterest: number;
  maxRisk: 'BREACHED' | 'AT_RISK' | 'SAFE';
  unpaidCount: number;
  latestDate: string;
}

@Component({
  selector: 'app-compliance-view',
  standalone: true,
  imports: [CommonModule, FormsModule, ButtonModule, TableModule, PanelModule, MenuModule, TooltipModule],
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
  runId = input<string | null>(null);
  filename = input<string>('');
  date = input<string>('');
  showExportAll = input<boolean>(true);
  showBack = input<boolean>(false);

  exportAll = output<string>();
  back = output<void>();
  exportLedger = output<{ ledgerName: string; summary: LedgerResult['summary'] }>();

  // ── Filter & Search State ──
  searchQuery = signal('');
  activeStatusFilter = signal<StatusFilter>('ALL');
  activeRiskFilter = signal<RiskFilter>('ALL');
  sortBy = signal<SortOption>('risk');

  // Track expanded/collapsed state per supplier
  expandedSuppliers: Record<string, boolean> = {};

  // Quick Metric Filter (Interactive Drill-down)
  activeMetricFilter = signal<'ALL' | 'ITC' | 'INTEREST'>('ALL');

  /** Per-file ledger tab: -1 = All Files, 0..N = individual ledger */
  selectedLedgerIndex = signal(-1);

  /** The currently active ledger(s) based on tab selection */
  get activeResults() {
    const idx = this.selectedLedgerIndex();
    const all = this.results();
    return idx === -1 || all.length <= 1 ? all : [all[idx]];
  }

  /** Switch to a different ledger tab and reset filters */
  selectLedger(idx: number) {
    this.selectedLedgerIndex.set(idx);
    this.searchQuery.set('');
    this.activeStatusFilter.set('ALL');
    this.activeRiskFilter.set('ALL');
    this.expandedSuppliers = {};
  }

  /** Toggle to show all columns or simplified view */
  showAllColumns = false;

  /** Export dropdown menu items */
  exportMenuItems: MenuItem[] = [
    {
      label: 'Issues Report',
      icon: 'pi pi-exclamation-triangle',
      command: () => this.exportAll.emit('issues'),
      tooltip: 'Export only late/unpaid transactions'
    },
    {
      label: 'Complete Report',
      icon: 'pi pi-list',
      command: () => this.exportAll.emit('complete'),
      tooltip: 'Export all transactions including on-time payments'
    },
    {
      label: 'GSTR-3B Summary',
      icon: 'pi pi-file-export',
      command: () => this.exportAll.emit('gstr3b'),
      tooltip: 'Export GSTR-3B Table 4(B)(2) Reversal Summary'
    }
  ];

  // ══════════════════════════════════════════════════════
  //  COMPUTED PROPERTIES — KPIs
  // ══════════════════════════════════════════════════════

  get totalItcReversal(): number {
    return this.activeResults.reduce((s, r) => s + r.summary.totalItcReversal, 0);
  }

  get totalInterest(): number {
    return this.activeResults.reduce((s, r) => s + r.summary.totalInterest, 0);
  }

  get totalLedgerCount(): number {
    const suppliers = new Set<string>();
    this.activeResults.forEach(lr =>
      lr.summary.details.forEach(d => suppliers.add(d.supplier))
    );
    return Math.max(1, suppliers.size);
  }

  get totalTransactions(): number {
    return this.activeResults.reduce((s, r) => s + r.summary.details.length, 0);
  }

  // ══════════════════════════════════════════════════════
  //  FILTERED + SORTED SUPPLIER GROUPS
  // ══════════════════════════════════════════════════════

  /** Filter out PAID_ON_TIME and SAFE-unpaid entries for UI display */
  private getIssueDetails(details: InterestRow[]): InterestRow[] {
    return details.filter(d =>
      d.status !== 'PAID_ON_TIME' &&
      !(d.riskCategory === 'SAFE' && d.interest === 0 && d.itcAmount === 0)
    );
  }

  /** Build supplier groups from active (tab-scoped) results */
  private getAllSupplierGroups(): SupplierGroup[] {
    const map = new Map<string, InterestRow[]>();
    for (const lr of this.activeResults) {
      const issueDetails = this.getIssueDetails(lr.summary.details);
      for (const row of issueDetails) {
        const list = map.get(row.supplier) ?? [];
        list.push(row);
        map.set(row.supplier, list);
      }
    }

    return Array.from(map.entries()).map(([supplier, rows]) => ({
      supplier,
      rows,
      totalItc: rows.reduce((s, r) => s + r.itcAmount, 0),
      totalInterest: rows.reduce((s, r) => s + r.interest, 0),
      maxRisk: this.getMaxRisk(rows),
      unpaidCount: rows.filter(r => r.status === 'UNPAID').length,
      latestDate: rows.reduce((latest, r) =>
        r.purchaseDate > latest ? r.purchaseDate : latest, rows[0]?.purchaseDate || ''),
    }));
  }

  private getMaxRisk(rows: InterestRow[]): 'BREACHED' | 'AT_RISK' | 'SAFE' {
    if (rows.some(r => r.riskCategory === 'BREACHED')) return 'BREACHED';
    if (rows.some(r => r.riskCategory === 'AT_RISK')) return 'AT_RISK';
    return 'SAFE';
  }

  /** The main computed list that the template iterates */
  get filteredSupplierGroups(): SupplierGroup[] {
    let groups = this.getAllSupplierGroups();

    // 1. Search filter
    const query = this.searchQuery().trim().toLowerCase();
    if (query) {
      groups = groups.filter(g => g.supplier.toLowerCase().includes(query));
    }

    // 2. Status filter — filter rows within each group
    const statusFilter = this.activeStatusFilter();
    if (statusFilter !== 'ALL') {
      groups = groups.map(g => ({
        ...g,
        rows: g.rows.filter(r => r.status === statusFilter),
      })).filter(g => g.rows.length > 0);
    }

    // 3. Metric filter (Interactive Drill-down)
    const metricFilter = this.activeMetricFilter();
    if (metricFilter === 'ITC') {
      groups = groups.map(g => ({
        ...g,
        rows: g.rows.filter(r => r.itcAmount > 0),
      })).filter(g => g.rows.length > 0);
    } else if (metricFilter === 'INTEREST') {
      groups = groups.map(g => ({
        ...g,
        rows: g.rows.filter(r => r.interest > 0),
      })).filter(g => g.rows.length > 0);
    }

    // 3. Risk filter — filter rows within each group
    const riskFilter = this.activeRiskFilter();
    if (riskFilter !== 'ALL') {
      groups = groups.map(g => ({
        ...g,
        rows: g.rows.filter(r => r.riskCategory === riskFilter),
      })).filter(g => g.rows.length > 0);
    }

    // 4. Sort
    const sort = this.sortBy();
    switch (sort) {
      case 'risk':
        groups.sort((a, b) => {
          const riskOrder = { BREACHED: 0, AT_RISK: 1, SAFE: 2 };
          const diff = riskOrder[a.maxRisk] - riskOrder[b.maxRisk];
          return diff !== 0 ? diff : (b.totalItc + b.totalInterest) - (a.totalItc + a.totalInterest);
        });
        break;
      case 'amount-desc':
        groups.sort((a, b) => (b.totalItc + b.totalInterest) - (a.totalItc + a.totalInterest));
        break;
      case 'amount-asc':
        groups.sort((a, b) => (a.totalItc + a.totalInterest) - (b.totalItc + b.totalInterest));
        break;
      case 'date-desc':
        groups.sort((a, b) => b.latestDate.localeCompare(a.latestDate));
        break;
      case 'date-asc':
        groups.sort((a, b) => a.latestDate.localeCompare(b.latestDate));
        break;
    }

    return groups;
  }

  get matchingSupplierCount(): number {
    return this.filteredSupplierGroups.length;
  }

  get hasActiveFilters(): boolean {
    return this.searchQuery().trim() !== '' ||
      this.activeStatusFilter() !== 'ALL' ||
      this.activeRiskFilter() !== 'ALL' ||
      this.activeMetricFilter() !== 'ALL';
  }

  // ══════════════════════════════════════════════════════
  //  FILTER TOGGLE ACTIONS
  // ══════════════════════════════════════════════════════

  toggleStatusFilter(status: StatusFilter): void {
    this.activeStatusFilter.set(this.activeStatusFilter() === status ? 'ALL' : status);
  }

  toggleRiskFilter(risk: RiskFilter): void {
    this.activeRiskFilter.set(this.activeRiskFilter() === risk ? 'ALL' : risk);
  }

  toggleMetricFilter(metric: 'ITC' | 'INTEREST'): void {
    this.activeMetricFilter.set(this.activeMetricFilter() === metric ? 'ALL' : metric);
  }

  clearAllFilters(): void {
    this.searchQuery.set('');
    this.activeStatusFilter.set('ALL');
    this.activeRiskFilter.set('ALL');
    this.activeMetricFilter.set('ALL');
    this.sortBy.set('risk');
  }

  // ══════════════════════════════════════════════════════
  //  RISK COUNTS (for KPI cards)
  // ══════════════════════════════════════════════════════

  getBreachedCount(): number {
    return this.results().flatMap(r => this.getIssueDetails(r.summary.details))
      .filter(d => d.riskCategory === 'BREACHED').length;
  }

  getAtRiskCount(): number {
    return this.results().flatMap(r => this.getIssueDetails(r.summary.details))
      .filter(d => d.riskCategory === 'AT_RISK').length;
  }

  getSafeCount(): number {
    const total = this.results().flatMap(r => this.getIssueDetails(r.summary.details)).length;
    return total - this.getBreachedCount() - this.getAtRiskCount();
  }

  // Donut chart calculations
  getDonutSegment(type: 'breached' | 'at-risk'): string {
    const total = Math.max(1, this.getBreachedCount() + this.getAtRiskCount() + this.getSafeCount());
    const count = type === 'breached' ? this.getBreachedCount() : this.getAtRiskCount();
    const percentage = (count / total) * 100;
    return `${percentage} ${100 - percentage}`;
  }

  getDonutOffset(type: 'at-risk'): number {
    const breachedPercent = (this.getBreachedCount() / Math.max(1, this.getBreachedCount() + this.getAtRiskCount() + this.getSafeCount())) * 100;
    return 25 - breachedPercent;
  }

  // ══════════════════════════════════════════════════════
  //  SUPPLIER EXPANSION
  // ══════════════════════════════════════════════════════

  toggleSupplier(supplier: string): void {
    this.expandedSuppliers[supplier] = !this.expandedSuppliers[supplier];
  }

  isSupplierExpanded(supplier: string): boolean {
    // Default: expand first 5 suppliers, collapse the rest
    if (this.expandedSuppliers[supplier] === undefined) {
      const groups = this.filteredSupplierGroups;
      const idx = groups.findIndex(g => g.supplier === supplier);
      return idx < 5;
    }
    return this.expandedSuppliers[supplier];
  }

  // ══════════════════════════════════════════════════════
  //  FORMATTING HELPERS
  // ══════════════════════════════════════════════════════

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

  humanizeDays(delayDays: number): string {
    const overBy = delayDays - 180;
    if (overBy <= 0) return 'On time';
    if (overBy >= 730) return `${Math.floor(overBy / 365)}+ yrs late`;
    if (overBy >= 365) return '1+ yr late';
    return `${overBy} days late`;
  }

  humanizeOverdue(daysToDeadline: number): string {
    const over = Math.abs(daysToDeadline);
    if (over >= 730) return `Overdue by ${Math.floor(over / 365)}+ years`;
    if (over >= 365) return 'Overdue by 1+ year';
    if (over >= 30) return `Overdue by ${Math.floor(over / 30)} months`;
    return `Overdue by ${over} days`;
  }

  humanizeStatus(status: string | undefined): { label: string; icon: string } {
    switch (status) {
      case 'PAID_LATE': return { label: 'Paid Late', icon: 'pi pi-clock' };
      case 'UNPAID': return { label: 'Not Paid', icon: 'pi pi-exclamation-triangle' };
      case 'PAID_ON_TIME': return { label: 'On Time', icon: 'pi pi-check-circle' };
      default: return { label: 'Pending', icon: 'pi pi-info-circle' };
    }
  }

  toggleColumns(): void {
    this.showAllColumns = !this.showAllColumns;
  }

  getInterestTooltip(row: InterestRow): string {
    if (!row.itcAvailmentDate) return 'Interest calculated at 18% p.a. (Sec 50)';
    const startDate = this.formatDate(row.itcAvailmentDate);
    const endDate = row.paymentDate ? this.formatDate(row.paymentDate) : 'Today';
    return `Interest calculated from ${startDate} (Estimated Availment) to ${endDate} (Reversal) as per Sec 50 of CGST Act.`;
  }
}
