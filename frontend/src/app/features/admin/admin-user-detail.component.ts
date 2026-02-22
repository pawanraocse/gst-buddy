import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { InputTextModule } from 'primeng/inputtext';
import { InputNumberModule } from 'primeng/inputnumber';
import { DialogModule } from 'primeng/dialog';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { ToastModule } from 'primeng/toast';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { MessageService, ConfirmationService } from 'primeng/api';
import { forkJoin } from 'rxjs';
import {
  AdminApiService, AdminUserDetail, AdminTransaction, RoleDto
} from '../../core/services/admin-api.service';

@Component({
  selector: 'app-admin-user-detail',
  standalone: true,
  imports: [
    CommonModule, FormsModule, CardModule, ButtonModule, TagModule,
    InputTextModule, InputNumberModule, DialogModule, SelectModule,
    TableModule, ToastModule, ConfirmDialogModule
  ],
  providers: [MessageService, ConfirmationService],
  template: `
    <div class="user-detail">
      <p-toast></p-toast>
      <p-confirmDialog></p-confirmDialog>

      <!-- Back button -->
      <button pButton icon="pi pi-arrow-left" label="Back to Users" [text]="true"
              class="mb-3" (click)="router.navigate(['/app/admin/users'])"></button>

      @if (loading()) {
        <div class="loading-state"><i class="pi pi-spin pi-spinner" style="font-size: 2rem"></i></div>
      } @else if (user(); as u) {
        <!-- User Profile Card -->
        <div class="profile-card">
          <div class="profile-header">
            <div class="avatar">
              <i class="pi pi-user"></i>
            </div>
            <div class="profile-info">
              <h2>{{ u.name || u.email.split('@')[0] }}</h2>
              <span class="email">{{ u.email }}</span>
              <div class="profile-meta">
                <p-tag [value]="u.status" [severity]="statusSeverity(u.status)" />
                <span class="meta-item"><i class="pi pi-calendar"></i> Joined {{ u.createdAt | date:'mediumDate' }}</span>
                @if (u.lastLoginAt) {
                  <span class="meta-item"><i class="pi pi-clock"></i> Last login {{ u.lastLoginAt | date:'medium' }}</span>
                }
              </div>
            </div>
            <div class="profile-actions">
              @if (u.status === 'DISABLED' || u.status === 'SUSPENDED') {
                <button pButton icon="pi pi-check-circle" label="Enable" severity="success"
                        [outlined]="true" (click)="enableUser()"></button>
              }
              @if (u.status === 'ACTIVE') {
                <button pButton icon="pi pi-ban" label="Suspend" severity="warn"
                        [outlined]="true" (click)="confirmSuspend()"></button>
              }
              <button pButton icon="pi pi-trash" label="Delete" severity="danger"
                      [outlined]="true" (click)="confirmDelete()"></button>
            </div>
          </div>
        </div>

        <!-- Roles & Wallet Row -->
        <div class="detail-grid">
          <!-- Roles -->
          <div class="detail-card">
            <div class="card-header">
              <h3><i class="pi pi-shield"></i> Roles</h3>
              <button pButton icon="pi pi-plus" label="Assign" [text]="true"
                      (click)="showRoleDialog = true"></button>
            </div>
            <div class="role-list">
              @for (role of u.roles; track role) {
                <div class="role-chip">
                  <span>{{ role }}</span>
                  <button class="chip-remove" (click)="removeRole(role)" pTooltip="Remove role">
                    <i class="pi pi-times"></i>
                  </button>
                </div>
              }
              @if (u.roles.length === 0) {
                <span class="text-secondary text-sm">No roles assigned</span>
              }
            </div>
          </div>

          <!-- Wallet -->
          <div class="detail-card">
            <div class="card-header">
              <h3><i class="pi pi-wallet"></i> Credit Wallet</h3>
              <button pButton icon="pi pi-plus" label="Grant" [text]="true"
                      (click)="showCreditDialog = true"></button>
            </div>
            @if (u.wallet) {
              <div class="wallet-stats">
                <div class="wallet-stat">
                  <span class="ws-value">{{ u.wallet.total }}</span>
                  <span class="ws-label">Total</span>
                </div>
                <div class="wallet-stat">
                  <span class="ws-value text-red-500">{{ u.wallet.used }}</span>
                  <span class="ws-label">Used</span>
                </div>
                <div class="wallet-stat">
                  <span class="ws-value text-green-600">{{ u.wallet.remaining }}</span>
                  <span class="ws-label">Remaining</span>
                </div>
              </div>
            } @else {
              <span class="text-secondary text-sm">No wallet data</span>
            }
          </div>
        </div>

        <!-- Transaction History -->
        <div class="detail-card full-width">
          <div class="card-header">
            <h3><i class="pi pi-history"></i> Transaction History</h3>
          </div>
          <p-table [value]="transactions()" [paginator]="true" [rows]="10"
                   [loading]="txLoading()" styleClass="p-datatable-sm" [rowHover]="true">
            <ng-template pTemplate="header">
              <tr>
                <th>Date</th>
                <th>Type</th>
                <th>Credits</th>
                <th>Balance After</th>
                <th>Description</th>
                <th>Reference</th>
              </tr>
            </ng-template>
            <ng-template pTemplate="body" let-tx>
              <tr>
                <td>{{ tx.createdAt | date:'medium' }}</td>
                <td>
                  <p-tag [value]="tx.type"
                         [severity]="tx.type === 'GRANT' || tx.type === 'PURCHASE' ? 'success' : 'warn'" />
                </td>
                <td class="font-mono"
                    [class.text-green-600]="tx.type === 'GRANT' || tx.type === 'PURCHASE'"
                    [class.text-red-500]="tx.type === 'CONSUME' || tx.type === 'REVOKE'">
                  {{ (tx.type === 'GRANT' || tx.type === 'PURCHASE') ? '+' : '-' }}{{ tx.credits }}
                </td>
                <td class="font-mono">{{ tx.balanceAfter }}</td>
                <td>{{ tx.description || '—' }}</td>
                <td class="text-sm text-secondary">{{ tx.referenceType }}{{ tx.referenceId ? ':' + tx.referenceId : '' }}</td>
              </tr>
            </ng-template>
            <ng-template pTemplate="emptymessage">
              <tr><td colspan="6" class="text-center p-4 text-secondary">No transactions</td></tr>
            </ng-template>
          </p-table>
        </div>
      }

      <!-- Assign Role Dialog -->
      <p-dialog header="Assign Role" [(visible)]="showRoleDialog" [modal]="true"
                [style]="{width: '400px'}" [closable]="true">
        <div class="flex flex-column gap-3 pt-2">
          <p-select [options]="availableRoles()" optionLabel="name" optionValue="id"
                    [(ngModel)]="selectedRoleId" placeholder="Select a role"
                    styleClass="w-full"></p-select>
          <div class="flex justify-content-end gap-2">
            <button pButton label="Cancel" [text]="true" (click)="showRoleDialog = false"></button>
            <button pButton label="Assign" icon="pi pi-check"
                    [disabled]="!selectedRoleId" (click)="assignRole()"></button>
          </div>
        </div>
      </p-dialog>

      <!-- Grant Credits Dialog -->
      <p-dialog header="Grant / Revoke Credits" [(visible)]="showCreditDialog" [modal]="true"
                [style]="{width: '400px'}" [closable]="true">
        <div class="flex flex-column gap-3 pt-2">
          <div class="flex flex-column gap-1">
            <label class="font-medium">Credits</label>
            <p-inputNumber [(ngModel)]="creditAmount" [min]="1" [showButtons]="true"
                           styleClass="w-full"></p-inputNumber>
          </div>
          <div class="flex flex-column gap-1">
            <label class="font-medium">Description (optional)</label>
            <input pInputText [(ngModel)]="creditDescription" placeholder="e.g. Bonus credits" class="w-full" />
          </div>
          <div class="flex justify-content-end gap-2">
            <button pButton label="Revoke" severity="danger" [outlined]="true"
                    [disabled]="!creditAmount" (click)="revokeCredits()"></button>
            <button pButton label="Grant" icon="pi pi-plus"
                    [disabled]="!creditAmount" (click)="grantCredits()"></button>
          </div>
        </div>
      </p-dialog>
    </div>
  `,
  styles: [`
    .user-detail { padding: 0.5rem; }
    .loading-state { display: flex; justify-content: center; padding: 4rem; }

    .profile-card {
      background: rgba(255,255,255,0.85); backdrop-filter: blur(12px);
      border-radius: 20px; padding: 1.5rem; border: 1px solid rgba(255,255,255,0.6);
      box-shadow: 0 4px 24px -4px rgba(0,0,0,0.06); margin-bottom: 1.25rem;
    }
    .profile-header { display: flex; align-items: flex-start; gap: 1.25rem; flex-wrap: wrap; }
    .avatar {
      width: 64px; height: 64px; border-radius: 20px;
      background: var(--primary-50, #eef2ff); color: var(--primary-500, #6366f1);
      display: flex; align-items: center; justify-content: center; font-size: 1.5rem; flex-shrink: 0;
    }
    .profile-info { flex: 1; min-width: 200px; }
    .profile-info h2 { margin: 0 0 0.25rem; font-size: 1.35rem; font-weight: 700; }
    .email { color: var(--text-secondary); font-size: 0.9rem; }
    .profile-meta { display: flex; gap: 0.75rem; align-items: center; margin-top: 0.75rem; flex-wrap: wrap; }
    .meta-item { font-size: 0.8rem; color: var(--text-secondary); display: flex; align-items: center; gap: 0.3rem; }
    .profile-actions { display: flex; gap: 0.5rem; flex-wrap: wrap; }

    .detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1.25rem; margin-bottom: 1.25rem; }
    @media (max-width: 768px) { .detail-grid { grid-template-columns: 1fr; } }

    .detail-card {
      background: rgba(255,255,255,0.85); backdrop-filter: blur(12px);
      border-radius: 20px; padding: 1.25rem; border: 1px solid rgba(255,255,255,0.6);
      box-shadow: 0 4px 24px -4px rgba(0,0,0,0.06);
    }
    .detail-card.full-width { margin-bottom: 1.25rem; }
    .card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
    .card-header h3 { margin: 0; font-size: 1rem; font-weight: 600; display: flex; align-items: center; gap: 0.5rem; }

    .role-list { display: flex; gap: 0.5rem; flex-wrap: wrap; }
    .role-chip {
      display: flex; align-items: center; gap: 0.35rem;
      padding: 0.35rem 0.75rem; border-radius: 20px;
      background: var(--primary-50, #eef2ff); color: var(--primary-700, #4338ca);
      font-size: 0.85rem; font-weight: 500;
    }
    .chip-remove {
      background: none; border: none; cursor: pointer; color: var(--red-500);
      padding: 0; font-size: 0.7rem; display: flex; opacity: 0.7;
      &:hover { opacity: 1; }
    }

    .wallet-stats { display: flex; gap: 2rem; }
    .wallet-stat { display: flex; flex-direction: column; }
    .ws-value { font-size: 1.5rem; font-weight: 700; font-family: 'SF Mono', monospace; }
    .ws-label { font-size: 0.75rem; color: var(--text-secondary); }

    .font-mono { font-family: 'SF Mono', 'Fira Code', monospace; }
  `]
})
export class AdminUserDetailComponent implements OnInit {
  readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly adminApi = inject(AdminApiService);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);

  loading = signal(true);
  txLoading = signal(true);
  user = signal<AdminUserDetail | null>(null);
  transactions = signal<AdminTransaction[]>([]);
  availableRoles = signal<RoleDto[]>([]);

  showRoleDialog = false;
  selectedRoleId: string | null = null;
  showCreditDialog = false;
  creditAmount: number = 10;
  creditDescription = '';

  private userId!: string;

  ngOnInit(): void {
    this.userId = this.route.snapshot.paramMap.get('userId')!;
    this.loadAll();
  }

  private loadAll(): void {
    this.loading.set(true);
    this.txLoading.set(true);

    forkJoin({
      user: this.adminApi.getUserDetail(this.userId),
      roles: this.adminApi.getRoles()
    }).subscribe({
      next: ({ user, roles }) => {
        this.user.set(user);
        this.availableRoles.set(roles);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });

    this.adminApi.getUserTransactions(this.userId).subscribe({
      next: txs => { this.transactions.set(txs); this.txLoading.set(false); },
      error: () => this.txLoading.set(false)
    });
  }

  enableUser(): void {
    this.adminApi.enableUser(this.userId).subscribe({
      next: () => { this.toast('success', 'Enabled', 'User has been enabled'); this.loadAll(); },
      error: () => this.toast('error', 'Error', 'Failed to enable user')
    });
  }

  confirmSuspend(): void {
    this.confirmationService.confirm({
      message: `Suspend this user? They will lose access immediately.`,
      header: 'Confirm Suspension',
      icon: 'pi pi-exclamation-triangle',
      acceptButtonStyleClass: 'p-button-warning',
      accept: () => {
        this.adminApi.suspendUser(this.userId).subscribe({
          next: () => { this.toast('warn', 'Suspended', 'User has been suspended'); this.loadAll(); },
          error: () => this.toast('error', 'Error', 'Failed to suspend user')
        });
      }
    });
  }

  confirmDelete(): void {
    this.confirmationService.confirm({
      message: 'Permanently delete this user? This cannot be undone.',
      header: 'Confirm Deletion',
      icon: 'pi pi-exclamation-triangle',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.adminApi.deleteUser(this.userId).subscribe({
          next: () => {
            this.toast('success', 'Deleted', 'User has been deleted');
            this.router.navigate(['/app/admin/users']);
          },
          error: () => this.toast('error', 'Error', 'Failed to delete user')
        });
      }
    });
  }

  assignRole(): void {
    if (!this.selectedRoleId) return;
    this.adminApi.assignRole(this.userId, this.selectedRoleId).subscribe({
      next: () => {
        this.toast('success', 'Assigned', `Role assigned successfully`);
        this.showRoleDialog = false;
        this.selectedRoleId = null;
        this.loadAll();
      },
      error: () => this.toast('error', 'Error', 'Failed to assign role')
    });
  }

  removeRole(roleId: string): void {
    this.adminApi.removeRole(this.userId, roleId).subscribe({
      next: () => { this.toast('success', 'Removed', `Role ${roleId} removed`); this.loadAll(); },
      error: () => this.toast('error', 'Error', 'Failed to remove role')
    });
  }

  grantCredits(): void {
    this.adminApi.grantCredits(this.userId, { credits: this.creditAmount, description: this.creditDescription || undefined }).subscribe({
      next: () => {
        this.toast('success', 'Granted', `${this.creditAmount} credits granted`);
        this.showCreditDialog = false;
        this.creditAmount = 10;
        this.creditDescription = '';
        this.loadAll();
      },
      error: () => this.toast('error', 'Error', 'Failed to grant credits')
    });
  }

  revokeCredits(): void {
    this.adminApi.revokeCredits(this.userId, { credits: this.creditAmount, description: this.creditDescription || undefined }).subscribe({
      next: () => {
        this.toast('warn', 'Revoked', `${this.creditAmount} credits revoked`);
        this.showCreditDialog = false;
        this.creditAmount = 10;
        this.creditDescription = '';
        this.loadAll();
      },
      error: () => this.toast('error', 'Error', 'Failed to revoke credits')
    });
  }

  statusSeverity(status: string): 'success' | 'info' | 'warn' | 'danger' {
    switch (status) {
      case 'ACTIVE': return 'success';
      case 'DISABLED': case 'SUSPENDED': return 'danger';
      default: return 'info';
    }
  }

  private toast(severity: string, summary: string, detail: string): void {
    this.messageService.add({ severity, summary, detail });
  }
}
