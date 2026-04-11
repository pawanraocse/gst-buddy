import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuditApiService } from '../../../core/services/audit-api.service';
import { AuditRunResponse } from '../../../shared/models/audit.model';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';
import { InputTextModule } from 'primeng/inputtext';
import { CardModule } from 'primeng/card';
import { finalize } from 'rxjs';

@Component({
  selector: 'app-audit-history',
  standalone: true,
  imports: [CommonModule, RouterModule, TableModule, TagModule, ButtonModule, TooltipModule, InputTextModule, CardModule],
  templateUrl: './audit-history.component.html',
  styleUrls: ['./audit-history.component.scss']
})
export class AuditHistoryComponent implements OnInit {
  private auditApi = inject(AuditApiService);

  runs = signal<AuditRunResponse[]>([]);
  totalRecords = signal(0);
  loading = signal(true);
  page = signal(0);
  size = signal(10);

  ngOnInit(): void {
    this.loadHistory();
  }

  loadHistory(event?: any) {
    this.loading.set(true);
    const p = event ? event.first / event.rows : 0;
    const s = event ? event.rows : 10;
    
    this.auditApi.listRuns(p, s).pipe(
      finalize(() => this.loading.set(false))
    ).subscribe({
      next: (res) => {
        this.runs.set(res.content);
        this.totalRecords.set(res.totalElements);
      },
      error: (err) => console.error('Failed to load audit history', err)
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
    return new Intl.DateTimeFormat('en-IN', {
      day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit'
    }).format(new Date(date));
  }

  deleteRun(event: Event, id: string) {
    event.stopPropagation();
    if (confirm('Are you sure you want to delete this audit run?')) {
        this.auditApi.deleteRun(id).subscribe(() => this.loadHistory());
    }
  }
}
