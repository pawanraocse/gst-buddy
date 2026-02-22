import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { ToastModule } from 'primeng/toast';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { MessageService, ConfirmationService } from 'primeng/api';
import { AdminApiService, AdminUserSummary } from '../../core/services/admin-api.service';

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [
    CommonModule, FormsModule, TableModule, ButtonModule, TagModule,
    InputTextModule, SelectModule, ToastModule, ConfirmDialogModule
  ],
  providers: [MessageService, ConfirmationService],
  template: `
    <div class="admin-users">
      <p-toast></p-toast>
      <p-confirmDialog></p-confirmDialog>

      <div class="page-header">
        <div>
          <h1>User Management</h1>
          <p class="text-secondary">View, suspend, enable, or delete any user on the platform.</p>
        </div>
      </div>

      <!-- Filters -->
      <div class="filter-bar">
        <span class="p-input-icon-left">
          <i class="pi pi-search"></i>
          <input pInputText type="text" placeholder="Search by email or name..."
                 [(ngModel)]="searchQuery" (input)="applyFilter()" class="search-input" />
        </span>
        <p-select [options]="statusOptions" [(ngModel)]="statusFilter"
                  (onChange)="loadUsers()" placeholder="All statuses" [showClear]="true"
                  styleClass="status-select"></p-select>
      </div>

      <!-- Table -->
      <div class="table-card">
        <p-table [value]="filteredUsers()" [loading]="loading()"
                 [paginator]="true" [rows]="15" [rowsPerPageOptions]="[15, 30, 50]"
                 [rowHover]="true" styleClass="p-datatable-sm">
          <ng-template pTemplate="header">
            <tr>
              <th pSortableColumn="email">Email <p-sortIcon field="email" /></th>
              <th pSortableColumn="name">Name <p-sortIcon field="name" /></th>
              <th pSortableColumn="status">Status <p-sortIcon field="status" /></th>
              <th>Roles</th>
              <th pSortableColumn="creditRemaining">Credits <p-sortIcon field="creditRemaining" /></th>
              <th pSortableColumn="createdAt">Created <p-sortIcon field="createdAt" /></th>
              <th>Actions</th>
            </tr>
          </ng-template>
          <ng-template pTemplate="body" let-user>
            <tr class="user-row" (click)="viewUser(user)">
              <td>{{ user.email }}</td>
              <td>{{ user.name || '—' }}</td>
              <td><p-tag [value]="user.status" [severity]="statusSeverity(user.status)" /></td>
              <td>
                @for (role of user.roles; track role) {
                  <p-tag [value]="role" severity="info" styleClass="mr-1 text-xs" />
                }
              </td>
              <td class="font-mono">{{ user.creditRemaining }}</td>
              <td>{{ user.createdAt | date:'mediumDate' }}</td>
              <td (click)="$event.stopPropagation()">
                <div class="flex gap-1">
                  @if (user.status === 'DISABLED' || user.status === 'SUSPENDED') {
                    <button pButton icon="pi pi-check-circle" severity="success"
                            [rounded]="true" [text]="true" pTooltip="Enable"
                            (click)="enableUser(user)"></button>
                  }
                  @if (user.status === 'ACTIVE') {
                    <button pButton icon="pi pi-ban" severity="warn"
                            [rounded]="true" [text]="true" pTooltip="Suspend"
                            (click)="confirmSuspend(user)"></button>
                  }
                  <button pButton icon="pi pi-trash" severity="danger"
                          [rounded]="true" [text]="true" pTooltip="Delete"
                          (click)="confirmDelete(user)"></button>
                </div>
              </td>
            </tr>
          </ng-template>
          <ng-template pTemplate="emptymessage">
            <tr>
              <td colspan="7" class="empty-state">
                <i class="pi pi-users" style="font-size: 2rem; opacity: 0.3"></i>
                <span>No users found.</span>
              </td>
            </tr>
          </ng-template>
        </p-table>
      </div>
    </div>
  `,
  styles: [`
    .admin-users { padding: 0.5rem; }
    .page-header { margin-bottom: 1.5rem; }
    .page-header h1 { font-size: 1.75rem; font-weight: 700; margin: 0 0 0.25rem; }
    .page-header p { margin: 0; font-size: 0.95rem; }

    .filter-bar {
      display: flex; gap: 1rem; margin-bottom: 1.25rem; flex-wrap: wrap;
    }
    .search-input { width: 320px; }
    :host ::ng-deep .status-select { min-width: 180px; }

    .table-card {
      background: rgba(255,255,255,0.85);
      backdrop-filter: blur(12px);
      border-radius: 20px;
      border: 1px solid rgba(255,255,255,0.6);
      box-shadow: 0 4px 24px -4px rgba(0,0,0,0.06);
      overflow: hidden;
    }
    .user-row { cursor: pointer; transition: background 0.15s; }
    .user-row:hover { background: rgba(99,102,241,0.04); }
    .font-mono { font-family: 'SF Mono', 'Fira Code', monospace; font-size: 0.9rem; }
    .empty-state {
      text-align: center; padding: 3rem !important;
      display: flex; flex-direction: column; align-items: center; gap: 0.5rem;
      color: var(--text-secondary);
    }
  `]
})
export class AdminUsersComponent implements OnInit {
  private readonly adminApi = inject(AdminApiService);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly router = inject(Router);

  loading = signal(true);
  users = signal<AdminUserSummary[]>([]);
  searchQuery = '';
  statusFilter: string | null = null;

  statusOptions = [
    { label: 'Active', value: 'ACTIVE' },
    { label: 'Disabled', value: 'DISABLED' },
    { label: 'Suspended', value: 'SUSPENDED' },
    { label: 'Invited', value: 'INVITED' }
  ];

  filteredUsers = computed(() => {
    const q = this.searchQuery.toLowerCase().trim();
    if (!q) return this.users();
    return this.users().filter(u =>
      u.email.toLowerCase().includes(q) ||
      (u.name?.toLowerCase().includes(q))
    );
  });

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading.set(true);
    this.adminApi.getUsers({ status: this.statusFilter ?? undefined }).subscribe({
      next: data => { this.users.set(data); this.loading.set(false); },
      error: () => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to load users' });
        this.loading.set(false);
      }
    });
  }

  applyFilter(): void {
    // filteredUsers is a computed signal — re-evaluates automatically
    // Force reactivity by re-setting same array so computed triggers
    this.users.update(u => [...u]);
  }

  viewUser(user: AdminUserSummary): void {
    this.router.navigate(['/app/admin/users', user.userId]);
  }

  enableUser(user: AdminUserSummary): void {
    this.adminApi.enableUser(user.userId).subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: 'Enabled', detail: `${user.email} has been enabled` });
        this.loadUsers();
      },
      error: () => this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to enable user' })
    });
  }

  confirmSuspend(user: AdminUserSummary): void {
    this.confirmationService.confirm({
      message: `Suspend user ${user.email}? They will lose access immediately.`,
      header: 'Confirm Suspension',
      icon: 'pi pi-exclamation-triangle',
      acceptButtonStyleClass: 'p-button-warning',
      accept: () => {
        this.adminApi.suspendUser(user.userId).subscribe({
          next: () => {
            this.messageService.add({ severity: 'warn', summary: 'Suspended', detail: `${user.email} has been suspended` });
            this.loadUsers();
          },
          error: () => this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to suspend user' })
        });
      }
    });
  }

  confirmDelete(user: AdminUserSummary): void {
    this.confirmationService.confirm({
      message: `Permanently delete user ${user.email}? This cannot be undone.`,
      header: 'Confirm Deletion',
      icon: 'pi pi-exclamation-triangle',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.adminApi.deleteUser(user.userId).subscribe({
          next: () => {
            this.messageService.add({ severity: 'success', summary: 'Deleted', detail: `${user.email} has been deleted` });
            this.loadUsers();
          },
          error: () => this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to delete user' })
        });
      }
    });
  }

  statusSeverity(status: string): 'success' | 'info' | 'warn' | 'danger' {
    switch (status) {
      case 'ACTIVE': return 'success';
      case 'DISABLED': case 'SUSPENDED': return 'danger';
      case 'INVITED': return 'info';
      default: return 'info';
    }
  }
}
