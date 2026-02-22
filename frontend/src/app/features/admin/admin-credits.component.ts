import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { InputTextModule } from 'primeng/inputtext';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { AdminApiService, AdminUserSummary, AdminDashboardStats } from '../../core/services/admin-api.service';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-admin-credits',
  standalone: true,
  imports: [CommonModule, FormsModule, TableModule, ButtonModule, TagModule, InputTextModule, ToastModule],
  providers: [MessageService],
  template: `
    <div class="admin-credits">
      <p-toast></p-toast>

      <div class="page-header">
        <h1>Credit Overview</h1>
        <p class="text-secondary">Platform-wide credit usage and per-user wallet breakdown.</p>
      </div>

      @if (stats(); as s) {
        <!-- Summary Cards -->
        <div class="summary-grid">
          <div class="summary-card">
            <div class="sc-icon green"><i class="pi pi-plus-circle"></i></div>
            <div class="sc-body">
              <span class="sc-value">{{ formatNumber(s.totalCreditsGranted) }}</span>
              <span class="sc-label">Total Granted</span>
            </div>
          </div>
          <div class="summary-card">
            <div class="sc-icon orange"><i class="pi pi-bolt"></i></div>
            <div class="sc-body">
              <span class="sc-value">{{ formatNumber(s.totalCreditsConsumed) }}</span>
              <span class="sc-label">Total Consumed</span>
            </div>
          </div>
          <div class="summary-card">
            <div class="sc-icon blue"><i class="pi pi-wallet"></i></div>
            <div class="sc-body">
              <span class="sc-value">{{ formatNumber(s.totalCreditsGranted - s.totalCreditsConsumed) }}</span>
              <span class="sc-label">Outstanding Balance</span>
            </div>
          </div>
          <div class="summary-card">
            <div class="sc-icon purple"><i class="pi pi-receipt"></i></div>
            <div class="sc-body">
              <span class="sc-value">{{ s.totalTransactions }}</span>
              <span class="sc-label">Transactions</span>
            </div>
          </div>
        </div>
      }

      <!-- User Wallets Table -->
      <div class="table-card">
        <div class="table-header">
          <h3>Per-User Wallet Breakdown</h3>
          <span class="p-input-icon-left">
            <i class="pi pi-search"></i>
            <input pInputText type="text" placeholder="Search users..."
                   [(ngModel)]="search" (input)="filterUsers()" class="search-input" />
          </span>
        </div>
        <p-table [value]="displayUsers()" [loading]="loading()"
                 [paginator]="true" [rows]="20" [rowsPerPageOptions]="[20, 50, 100]"
                 [rowHover]="true" styleClass="p-datatable-sm"
                 [globalFilterFields]="['email','name']">
          <ng-template pTemplate="header">
            <tr>
              <th pSortableColumn="email">Email <p-sortIcon field="email" /></th>
              <th pSortableColumn="name">Name <p-sortIcon field="name" /></th>
              <th pSortableColumn="status">Status <p-sortIcon field="status" /></th>
              <th pSortableColumn="creditRemaining" style="text-align:right">Credits Remaining <p-sortIcon field="creditRemaining" /></th>
              <th>Actions</th>
            </tr>
          </ng-template>
          <ng-template pTemplate="body" let-user>
            <tr>
              <td>{{ user.email }}</td>
              <td>{{ user.name || '—' }}</td>
              <td><p-tag [value]="user.status" [severity]="statusSev(user.status)" /></td>
              <td class="font-mono" style="text-align:right"
                  [class.text-red-500]="user.creditRemaining <= 0"
                  [class.text-green-600]="user.creditRemaining > 0">
                {{ user.creditRemaining }}
              </td>
              <td>
                <button pButton icon="pi pi-eye" [text]="true" [rounded]="true"
                        pTooltip="View Details"
                        (click)="router.navigate(['/app/admin/users', user.userId])"></button>
              </td>
            </tr>
          </ng-template>
          <ng-template pTemplate="emptymessage">
            <tr>
              <td colspan="5" class="empty-state">No users found</td>
            </tr>
          </ng-template>
        </p-table>
      </div>
    </div>
  `,
  styles: [`
    .admin-credits { padding: 0.5rem; }
    .page-header { margin-bottom: 1.5rem; }
    .page-header h1 { font-size: 1.75rem; font-weight: 700; margin: 0 0 0.25rem; }
    .page-header p { margin: 0; font-size: 0.95rem; }

    .summary-grid {
      display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 1rem; margin-bottom: 1.5rem;
    }
    .summary-card {
      background: rgba(255,255,255,0.85); backdrop-filter: blur(12px);
      border-radius: 16px; padding: 1.25rem; border: 1px solid rgba(255,255,255,0.6);
      box-shadow: 0 2px 16px -4px rgba(0,0,0,0.05);
      display: flex; align-items: center; gap: 1rem;
    }
    .sc-icon {
      width: 40px; height: 40px; border-radius: 12px;
      display: flex; align-items: center; justify-content: center; font-size: 1.1rem;
    }
    .sc-icon.green { background: #d1fae5; color: #059669; }
    .sc-icon.orange { background: #fef3c7; color: #d97706; }
    .sc-icon.blue { background: #dbeafe; color: #2563eb; }
    .sc-icon.purple { background: #ede9fe; color: #7c3aed; }
    .sc-body { display: flex; flex-direction: column; }
    .sc-value { font-size: 1.35rem; font-weight: 700; font-family: 'SF Mono', monospace; }
    .sc-label { font-size: 0.8rem; color: var(--text-secondary); }

    .table-card {
      background: rgba(255,255,255,0.85); backdrop-filter: blur(12px);
      border-radius: 20px; border: 1px solid rgba(255,255,255,0.6);
      box-shadow: 0 4px 24px -4px rgba(0,0,0,0.06); overflow: hidden;
    }
    .table-header {
      display: flex; justify-content: space-between; align-items: center;
      padding: 1.25rem 1.5rem; flex-wrap: wrap; gap: 1rem;
    }
    .table-header h3 { margin: 0; font-size: 1.05rem; font-weight: 600; }
    .search-input { width: 260px; }
    .font-mono { font-family: 'SF Mono', 'Fira Code', monospace; }
    .empty-state { text-align: center; padding: 2rem !important; color: var(--text-secondary); }
  `]
})
export class AdminCreditsComponent implements OnInit {
  readonly router = inject(Router);
  private readonly adminApi = inject(AdminApiService);
  private readonly messageService = inject(MessageService);

  loading = signal(true);
  stats = signal<AdminDashboardStats | null>(null);
  allUsers = signal<AdminUserSummary[]>([]);
  displayUsers = signal<AdminUserSummary[]>([]);
  search = '';

  ngOnInit(): void {
    forkJoin({
      stats: this.adminApi.getDashboardStats(),
      users: this.adminApi.getUsers()
    }).subscribe({
      next: ({ stats, users }) => {
        this.stats.set(stats);
        this.allUsers.set(users);
        this.displayUsers.set(users);
        this.loading.set(false);
      },
      error: () => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to load data' });
        this.loading.set(false);
      }
    });
  }

  filterUsers(): void {
    const q = this.search.toLowerCase().trim();
    if (!q) {
      this.displayUsers.set(this.allUsers());
      return;
    }
    this.displayUsers.set(
      this.allUsers().filter(u =>
        u.email.toLowerCase().includes(q) || u.name?.toLowerCase().includes(q)
      )
    );
  }

  formatNumber(n: number): string {
    return new Intl.NumberFormat('en-IN').format(n);
  }

  statusSev(status: string): 'success' | 'info' | 'warn' | 'danger' {
    switch (status) {
      case 'ACTIVE': return 'success';
      case 'DISABLED': case 'SUSPENDED': return 'danger';
      default: return 'info';
    }
  }
}
