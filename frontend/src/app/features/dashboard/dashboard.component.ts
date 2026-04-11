import { Component, inject, signal, OnInit, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { AuditApiService } from '../../core/services/audit-api.service';
import { CreditApiService } from '../../core/services/credit-api.service';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { SkeletonModule } from 'primeng/skeleton';
import { AuditRuleInfo, AuditRunResponse } from '../../shared/models/audit.model';
import { forkJoin, finalize } from 'rxjs';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule, 
    RouterModule, 
    CardModule, 
    ButtonModule, 
    TableModule, 
    TagModule, 
    TooltipModule, 
    SkeletonModule
  ],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {
  authService = inject(AuthService);
  private auditApi = inject(AuditApiService);
  private creditApi = inject(CreditApiService);

  loading = signal(true);
  rules = signal<AuditRuleInfo[]>([]);
  recentRuns = signal<AuditRunResponse[]>([]);
  wallet = signal({ total: 0, used: 0, remaining: 0 });

  // Dashboard Stats (Calculated from recent runs/wallet)
  stats = computed(() => [
    { label: 'Total Audits', value: this.recentRuns().length, icon: 'pi pi-check-circle', color: 'var(--primary-500)' },
    { label: 'Rules Available', value: this.rules().length, icon: 'pi pi-shield', color: 'var(--success-500)' },
    { label: 'Remaining Credits', value: this.wallet().remaining, icon: 'pi pi-bolt', color: 'var(--warning-500)' },
    { label: 'Credits Used', value: this.wallet().used, icon: 'pi pi-chart-line', color: 'var(--info-500)' }
  ]);

  ngOnInit(): void {
    this.refreshData();
  }

  refreshData() {
    this.loading.set(true);
    forkJoin({
      rules: this.auditApi.getAvailableRules(),
      runs: this.auditApi.listRuns(0, 5),
      wallet: this.creditApi.getWallet()
    }).pipe(
      finalize(() => this.loading.set(false))
    ).subscribe({
      next: (data) => {
        this.rules.set(data.rules);
        this.recentRuns.set(data.runs.content);
        this.wallet.set(data.wallet);
      },
      error: (err) => console.error('Dashboard load failed', err)
    });
  }

  getStatusSeverity(status: string): 'success' | 'warn' | 'danger' | 'info' {
    switch (status?.toLowerCase()) {
      case 'completed': return 'success';
      case 'processing': return 'info';
      case 'failed': return 'danger';
      default: return 'warn';
    }
  }

  formatDate(date: string | Date): string {
    if (!date) return 'N/A';
    return new Intl.DateTimeFormat('en-IN', {
      day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit'
    }).format(new Date(date));
  }
}
