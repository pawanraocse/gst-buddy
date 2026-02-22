import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { AdminApiService, AdminDashboardStats } from '../../core/services/admin-api.service';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, CardModule, ButtonModule, TagModule],
  template: `
    <div class="admin-dash">
      <div class="page-header">
        <h1>Platform Administration</h1>
        <p class="text-secondary">Overview of all users, credits, and revenue across the platform.</p>
      </div>

      @if (loading()) {
        <div class="loading-state">
          <i class="pi pi-spin pi-spinner" style="font-size: 2rem"></i>
        </div>
      } @else if (stats(); as s) {
        <!-- KPI Cards -->
        <div class="kpi-grid">
          <div class="kpi-card kpi-users" (click)="router.navigate(['/app/admin/users'])">
            <div class="kpi-icon"><i class="pi pi-users"></i></div>
            <div class="kpi-body">
              <span class="kpi-value">{{ s.totalUsers }}</span>
              <span class="kpi-label">Total Users</span>
            </div>
            <div class="kpi-footer">
              <span class="kpi-badge active">{{ s.activeUsers }} active</span>
              <span class="kpi-badge warn">{{ s.disabledUsers }} disabled</span>
            </div>
          </div>

          <div class="kpi-card kpi-credits">
            <div class="kpi-icon"><i class="pi pi-bolt"></i></div>
            <div class="kpi-body">
              <span class="kpi-value">{{ formatNumber(s.totalCreditsGranted) }}</span>
              <span class="kpi-label">Credits Granted</span>
            </div>
            <div class="kpi-footer">
              <span class="kpi-badge">{{ formatNumber(s.totalCreditsConsumed) }} consumed</span>
            </div>
          </div>

          <div class="kpi-card kpi-revenue">
            <div class="kpi-icon"><i class="pi pi-indian-rupee"></i></div>
            <div class="kpi-body">
              <span class="kpi-value">{{ formatCurrency(s.totalRevenueInr) }}</span>
              <span class="kpi-label">Total Revenue</span>
            </div>
            <div class="kpi-footer">
              <span class="kpi-badge">{{ s.totalTransactions }} transactions</span>
            </div>
          </div>

          <div class="kpi-card kpi-plans" (click)="router.navigate(['/app/admin/plans'])">
            <div class="kpi-icon"><i class="pi pi-credit-card"></i></div>
            <div class="kpi-body">
              <span class="kpi-value">{{ s.activePlans }}</span>
              <span class="kpi-label">Active Plans</span>
            </div>
            <div class="kpi-footer">
              <span class="kpi-badge">{{ s.invitedUsers }} invited users</span>
            </div>
          </div>
        </div>

        <!-- Quick Actions -->
        <div class="actions-section">
          <h3>Quick Actions</h3>
          <div class="actions-grid">
            <button pButton class="action-btn" (click)="router.navigate(['/app/admin/users'])">
              <i class="pi pi-users"></i>
              <span>Manage Users</span>
            </button>
            <button pButton class="action-btn" (click)="router.navigate(['/app/admin/plans'])">
              <i class="pi pi-credit-card"></i>
              <span>Manage Plans</span>
            </button>
            <button pButton class="action-btn" (click)="router.navigate(['/app/admin/credits'])">
              <i class="pi pi-wallet"></i>
              <span>Credit Overview</span>
            </button>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .admin-dash { padding: 0.5rem; }
    .page-header { margin-bottom: 2rem; }
    .page-header h1 { font-size: 1.75rem; font-weight: 700; margin: 0 0 0.25rem; }
    .page-header p { margin: 0; font-size: 0.95rem; }

    .loading-state { display: flex; justify-content: center; padding: 4rem; }

    .kpi-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
      gap: 1.25rem;
      margin-bottom: 2.5rem;
    }

    .kpi-card {
      background: rgba(255,255,255,0.85);
      backdrop-filter: blur(12px);
      border-radius: 20px;
      padding: 1.5rem;
      border: 1px solid rgba(255,255,255,0.6);
      box-shadow: 0 4px 24px -4px rgba(0,0,0,0.06);
      cursor: pointer;
      transition: transform 0.2s, box-shadow 0.2s;
      &:hover { transform: translateY(-2px); box-shadow: 0 8px 32px -4px rgba(0,0,0,0.1); }
    }

    .kpi-icon {
      width: 44px; height: 44px; border-radius: 14px;
      display: flex; align-items: center; justify-content: center;
      margin-bottom: 1rem; font-size: 1.25rem;
    }
    .kpi-users .kpi-icon { background: var(--primary-50, #eef2ff); color: var(--primary-500, #6366f1); }
    .kpi-credits .kpi-icon { background: #fef3c7; color: #d97706; }
    .kpi-revenue .kpi-icon { background: #d1fae5; color: #059669; }
    .kpi-plans .kpi-icon { background: #ede9fe; color: #7c3aed; }

    .kpi-body { display: flex; flex-direction: column; margin-bottom: 0.75rem; }
    .kpi-value { font-size: 1.75rem; font-weight: 700; line-height: 1.2; }
    .kpi-label { font-size: 0.85rem; color: var(--text-secondary, #64748b); margin-top: 0.25rem; }

    .kpi-footer { display: flex; gap: 0.5rem; flex-wrap: wrap; }
    .kpi-badge {
      font-size: 0.75rem; padding: 0.2rem 0.6rem;
      border-radius: 20px; background: #f1f5f9; color: #475569;
    }
    .kpi-badge.active { background: #d1fae5; color: #059669; }
    .kpi-badge.warn { background: #fee2e2; color: #dc2626; }

    .actions-section h3 { font-size: 1.1rem; font-weight: 600; margin-bottom: 1rem; }
    .actions-grid { display: flex; gap: 1rem; flex-wrap: wrap; }
    .action-btn {
      display: flex; align-items: center; gap: 0.75rem;
      padding: 1rem 1.5rem; border-radius: 14px;
      background: rgba(255,255,255,0.85); border: 1px solid rgba(0,0,0,0.08);
      color: var(--text-main, #1e293b); font-weight: 500;
      cursor: pointer; transition: all 0.2s;
      &:hover { background: var(--primary-50, #eef2ff); border-color: var(--primary-200, #c7d2fe); }
      i { font-size: 1.1rem; color: var(--primary-500, #6366f1); }
    }
  `]
})
export class AdminDashboardComponent implements OnInit {
  readonly router = inject(Router);
  private readonly adminApi = inject(AdminApiService);

  loading = signal(true);
  stats = signal<AdminDashboardStats | null>(null);

  ngOnInit(): void {
    this.adminApi.getDashboardStats().subscribe({
      next: s => { this.stats.set(s); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  formatNumber(n: number): string {
    return new Intl.NumberFormat('en-IN').format(n);
  }

  formatCurrency(n: number): string {
    return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(n);
  }
}
