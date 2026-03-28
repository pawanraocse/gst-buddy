import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { DialogModule } from 'primeng/dialog';
import { TextareaModule } from 'primeng/textarea';
import { FormsModule } from '@angular/forms';
import { SupportApiService, SupportTicketDto } from '../../../core/services/support-api.service';
import { MessageService } from 'primeng/api';
import { TooltipModule } from 'primeng/tooltip';

@Component({
  selector: 'app-admin-support',
  standalone: true,
  imports: [
    CommonModule,
    TableModule,
    TagModule,
    ButtonModule,
    InputTextModule,
    SelectModule,
    DialogModule,
    TextareaModule,
    FormsModule,
    TooltipModule
  ],
  template: `
    <div class="admin-support-container p-4">

      <!-- Header -->
      <div class="flex justify-content-between align-items-center mb-4">
        <div>
          <h1 class="text-2xl font-bold m-0 text-900">Support Queries</h1>
          <p class="text-500 text-sm m-0 mt-1">Manage user questions, bug reports, and feedback</p>
        </div>
        <button pButton icon="pi pi-refresh" label="Refresh" class="p-button-outlined p-button-sm"
          (click)="loadTickets()" [loading]="loading()">
        </button>
      </div>

      <!-- Filter Toolbar -->
      <div class="filter-bar flex flex-wrap gap-2 mb-4 p-3 surface-ground border-round-lg border-1 surface-border">
        <span class="p-input-icon-left flex-1" style="min-width: 200px">
          <i class="pi pi-search"></i>
          <input pInputText type="text" [(ngModel)]="filterSearch"
            (input)="onFilterChange()" placeholder="Search by email or user ID" class="w-full"/>
        </span>
        <p-select [options]="statusOptions" [(ngModel)]="filterStatus"
          (onChange)="onFilterChange()" placeholder="Status" styleClass="min-w-8rem">
        </p-select>
        <p-select [options]="categoryOptions" [(ngModel)]="filterCategory"
          (onChange)="onFilterChange()" placeholder="Category" styleClass="min-w-10rem">
        </p-select>
        <p-select [options]="enrolledOptions" [(ngModel)]="filterEnrolled"
          (onChange)="onFilterChange()" placeholder="User Type" styleClass="min-w-9rem">
        </p-select>
        <button pButton icon="pi pi-times" class="p-button-text p-button-secondary"
          pTooltip="Clear filters" (click)="clearFilters()">
        </button>
      </div>

      <!-- Table -->
      <p-table
        [value]="tickets()"
        [paginator]="true"
        [rows]="pageSize"
        [totalRecords]="totalRecords()"
        [lazy]="true"
        (onLazyLoad)="onLazyLoad($event)"
        [loading]="loading()"
        styleClass="p-datatable-sm p-datatable-striped shadow-1 border-round-lg"
        [rowHover]="true">

        <ng-template pTemplate="header">
          <tr>
            <th style="width: 140px">Date</th>
            <th>User / Email</th>
            <th style="width: 160px">Category</th>
            <th>Subject</th>
            <th style="width: 130px">Status</th>
            <th style="width: 80px" class="text-center">View</th>
          </tr>
        </ng-template>

        <ng-template pTemplate="body" let-ticket>
          <tr class="cursor-pointer" (click)="viewTicket(ticket)">
            <td class="text-500 text-sm">{{ ticket.createdAt | date:'dd MMM, HH:mm' }}</td>
            <td>
              <div class="flex flex-column gap-1">
                <span class="text-sm text-900 font-medium">{{ ticket.email || ticket.userId || '—' }}</span>
                <p-tag
                  [value]="ticket.isEnrolledUser ? 'Registered' : 'Guest'"
                  [severity]="ticket.isEnrolledUser ? 'success' : 'secondary'"
                  [style]="{ 'font-size': '0.7rem' }">
                </p-tag>
              </div>
            </td>
            <td>
              <p-tag [value]="ticket.category" severity="info" [style]="{ 'font-size': '0.75rem' }"></p-tag>
            </td>
            <td class="text-sm text-900 subject-cell" [pTooltip]="ticket.subject" tooltipPosition="top">
              {{ ticket.subject | slice:0:60 }}{{ ticket.subject?.length > 60 ? '...' : '' }}
            </td>
            <td>
              <p-tag [value]="ticket.status" [severity]="getStatusSeverity(ticket.status)"></p-tag>
            </td>
            <td class="text-center">
              <button pButton icon="pi pi-eye" class="p-button-text p-button-sm p-button-rounded"
                (click)="viewTicket(ticket); $event.stopPropagation()">
              </button>
            </td>
          </tr>
        </ng-template>

        <ng-template pTemplate="emptymessage">
          <tr>
            <td colspan="6" class="text-center py-5">
              <div class="flex flex-column align-items-center gap-2 text-500">
                <i class="pi pi-inbox text-4xl"></i>
                <span>No tickets found matching your filters</span>
              </div>
            </td>
          </tr>
        </ng-template>
      </p-table>

      <!-- Ticket Detail Dialog -->
      <p-dialog
        [(visible)]="displayDetail"
        [modal]="true"
        [draggable]="false"
        [style]="{ width: '640px', 'max-width': '95vw' }"
        [header]="'Ticket: ' + (selectedTicket()?.subject || '')"
        styleClass="ticket-detail-dialog">

        <div *ngIf="selectedTicket()" class="flex flex-column gap-3">

          <!-- Ticket Meta -->
          <div class="surface-ground p-3 border-round-lg border-1 surface-border">
            <div class="flex justify-content-between align-items-start gap-2 mb-2">
              <div class="flex flex-column gap-1">
                <span class="text-500 text-xs">From</span>
                <span class="font-semibold text-sm text-900">
                  {{ selectedTicket()?.email || selectedTicket()?.userId || 'Unknown' }}
                </span>
              </div>
              <div class="flex gap-2 align-items-center">
                <p-tag
                  [value]="selectedTicket()?.isEnrolledUser ? 'Registered' : 'Guest'"
                  [severity]="selectedTicket()?.isEnrolledUser ? 'success' : 'secondary'"
                  [style]="{ 'font-size': '0.7rem' }">
                </p-tag>
                <p-tag [value]="selectedTicket()?.status || ''" [severity]="getStatusSeverity(selectedTicket()?.status)"></p-tag>
              </div>
            </div>
            <div class="text-600 text-sm white-space-pre-wrap line-height-3 mt-2">{{ selectedTicket()?.description }}</div>
          </div>

          <!-- Thread -->
          <div *ngIf="(selectedTicket()?.replies?.length ?? 0) > 0" class="thread-section flex flex-column gap-2 max-h-20rem overflow-y-auto pr-1">
            <span class="text-xs text-500 uppercase letter-spacing-1 font-semibold">Conversation</span>
            @for (reply of selectedTicket()?.replies; track reply.id) {
              <div class="reply-bubble p-3 border-round-lg"
                [ngClass]="reply.isAdminReply ? 'reply-admin' : 'reply-user'">
                <div class="flex justify-content-between mb-1">
                  <span class="text-xs font-bold" [class]="reply.isAdminReply ? 'text-primary' : 'text-600'">
                    {{ reply.isAdminReply ? '🛡 Support Team' : '👤 User' }}
                  </span>
                  <span class="text-xs text-500">{{ reply.createdAt | date:'dd MMM, HH:mm' }}</span>
                </div>
                <p class="m-0 text-sm line-height-3">{{ reply.message }}</p>
              </div>
            }
          </div>

          <!-- Reply Box -->
          <div class="flex flex-column gap-2">
            <label class="font-semibold text-sm text-900">Reply to User</label>
            <textarea pTextarea [(ngModel)]="replyMessage" rows="3"
              placeholder="Type your response here..."
              class="w-full resize-none text-sm">
            </textarea>
          </div>

          <!-- Action Buttons -->
          <div class="flex justify-content-between align-items-center">
            <div class="flex gap-2">
              <button pButton label="In Progress" icon="pi pi-sync" [outlined]="true" size="small"
                *ngIf="selectedTicket()?.status === 'OPEN'"
                (click)="updateStatus('IN_PROGRESS')">
              </button>
              <button pButton label="Resolved" icon="pi pi-check-circle" severity="success" size="small"
                *ngIf="selectedTicket()?.status !== 'RESOLVED' && selectedTicket()?.status !== 'CLOSED'"
                (click)="updateStatus('RESOLVED')">
              </button>
              <button pButton label="Close" icon="pi pi-times-circle" [text]="true" size="small"
                *ngIf="selectedTicket()?.status !== 'CLOSED'"
                (click)="updateStatus('CLOSED')">
              </button>
            </div>
            <button pButton label="Send Reply" icon="pi pi-send"
              [loading]="replying()"
              [disabled]="!replyMessage || replyMessage.trim().length < 2"
              (click)="sendReply()">
            </button>
          </div>
        </div>
      </p-dialog>
    </div>
  `,
  styles: [`
    .filter-bar { background: var(--surface-ground); }
    .subject-cell { max-width: 260px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .thread-section { scrollbar-width: thin; }
    .thread-section::-webkit-scrollbar { width: 5px; }
    .thread-section::-webkit-scrollbar-thumb { background: var(--surface-border); border-radius: 10px; }
    .reply-bubble { border: 1px solid var(--surface-border); }
    .reply-admin { background: var(--blue-50); border-left: 3px solid var(--blue-400); }
    .reply-user { background: var(--surface-ground); border-left: 3px solid var(--surface-400); }
    .resize-none { resize: none; }
    .letter-spacing-1 { letter-spacing: 0.05em; }
    :host ::ng-deep .p-datatable tbody tr { cursor: pointer; }
  `]
})
export class AdminSupportComponent implements OnInit {
  private supportApi = inject(SupportApiService);
  private messageService = inject(MessageService);

  tickets = signal<SupportTicketDto[]>([]);
  totalRecords = signal(0);
  loading = signal(false);
  replying = signal(false);

  filterSearch = '';
  filterStatus = '';
  filterCategory = '';
  filterEnrolled: boolean | null = null;
  pageSize = 10;

  statusOptions = [
    { label: 'All Status', value: '' },
    { label: 'Open', value: 'OPEN' },
    { label: 'In Progress', value: 'IN_PROGRESS' },
    { label: 'Resolved', value: 'RESOLVED' },
    { label: 'Closed', value: 'CLOSED' }
  ];

  categoryOptions = [
    { label: 'All Categories', value: '' },
    { label: 'Technical Support', value: 'Technical Support' },
    { label: 'Billing & Subscriptions', value: 'Billing & Subscriptions' },
    { label: 'Feature Request', value: 'Feature Request' },
    { label: 'Bug Report', value: 'Bug Report' },
    { label: 'Other', value: 'Other' }
  ];

  enrolledOptions = [
    { label: 'All Users', value: null },
    { label: 'Registered', value: true },
    { label: 'Guest', value: false }
  ];

  displayDetail = false;
  selectedTicket = signal<SupportTicketDto | null>(null);
  replyMessage = '';

  ngOnInit() {
    this.loadTickets();
  }

  loadTickets(page = 0, size = this.pageSize) {
    this.loading.set(true);
    this.supportApi
      .getAdminTickets(this.filterStatus, this.filterSearch, this.filterCategory, this.filterEnrolled, page, size)
      .subscribe({
        next: (res: any) => {
          this.tickets.set(res.content);
          this.totalRecords.set(res.totalElements);
          this.loading.set(false);
        },
        error: () => {
          this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to load tickets' });
          this.loading.set(false);
        }
      });
  }

  onFilterChange() {
    this.loadTickets(0, this.pageSize);
  }

  clearFilters() {
    this.filterSearch = '';
    this.filterStatus = '';
    this.filterCategory = '';
    this.filterEnrolled = null;
    this.loadTickets(0, this.pageSize);
  }

  onLazyLoad(event: any) {
    const page = Math.floor((event.first ?? 0) / (event.rows ?? this.pageSize));
    this.loadTickets(page, event.rows ?? this.pageSize);
  }

  viewTicket(ticket: SupportTicketDto) {
    this.selectedTicket.set(ticket);
    this.displayDetail = true;
    this.replyMessage = '';
  }

  getStatusSeverity(status: string | undefined): any {
    const map: Record<string, string> = {
      OPEN: 'danger',
      IN_PROGRESS: 'warning',
      RESOLVED: 'success',
      CLOSED: 'secondary'
    };
    return map[status ?? ''] ?? 'info';
  }

  sendReply() {
    if (!this.replyMessage?.trim() || !this.selectedTicket()) return;
    const ticketId = this.selectedTicket()!.id;
    this.replying.set(true);

    this.supportApi.addAdminReply(ticketId, { message: this.replyMessage.trim() }).subscribe({
      next: (updated) => {
        this.tickets.update(list => list.map(t => t.id === ticketId ? updated : t));
        this.selectedTicket.set(updated);
        this.replyMessage = '';
        this.replying.set(false);
        this.messageService.add({ severity: 'success', summary: 'Reply Sent', detail: 'Your response has been sent to the user.' });
      },
      error: () => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to send reply. Please try again.' });
        this.replying.set(false);
      }
    });
  }

  updateStatus(status: string) {
    if (!this.selectedTicket()) return;
    const ticketId = this.selectedTicket()!.id;

    this.supportApi.updateTicketStatus(ticketId, status).subscribe({
      next: (updated) => {
        this.tickets.update(list => list.map(t => t.id === ticketId ? updated : t));
        this.selectedTicket.set(updated);
        this.messageService.add({ severity: 'success', summary: 'Updated', detail: `Ticket marked as ${status.replace('_', ' ')}` });
      },
      error: () => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to update status.' });
      }
    });
  }
}
