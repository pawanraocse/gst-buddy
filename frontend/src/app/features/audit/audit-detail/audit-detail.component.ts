import { Component, inject, signal, OnInit, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { AuditApiService } from '../../../core/services/audit-api.service';
import { AuditRunResponse, AuditFindingDto } from '../../../shared/models/audit.model';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { DividerModule } from 'primeng/divider';
import { finalize } from 'rxjs';

@Component({
  selector: 'app-audit-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, CardModule, ButtonModule, TableModule, TagModule, TooltipModule, DividerModule],
  templateUrl: './audit-detail.component.html',
  styleUrls: ['./audit-detail.component.scss']
})
export class AuditDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private auditApi = inject(AuditApiService);

  run = signal<AuditRunResponse | null>(null);
  loading = signal(true);
  exporting = signal(false);

  // Computed summary stats
  findings = computed<AuditFindingDto[]>(() => this.run()?.resultData?.findings || []);
  
  severityStats = computed(() => {
    const list = this.findings();
    return {
      critical: list.filter(f => f.severity === 'CRITICAL').length,
      high: list.filter(f => f.severity === 'HIGH').length,
      medium: list.filter(f => f.severity === 'MEDIUM').length,
      low: list.filter(f => f.severity === 'LOW').length,
      info: list.filter(f => f.severity === 'INFO').length
    };
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
        this.loadRun(id);
    }
  }

  loadRun(id: string) {
    this.loading.set(true);
    this.auditApi.getRun(id).pipe(
      finalize(() => this.loading.set(false))
    ).subscribe({
      next: (res) => this.run.set(res),
      error: (err) => console.error('Failed to load audit run', err)
    });
  }

  getSeverityBadge(sev: string): 'danger' | 'warn' | 'info' | 'success' | 'secondary' {
    switch (sev?.toUpperCase()) {
      case 'CRITICAL': return 'danger';
      case 'HIGH': return 'warn';
      case 'MEDIUM': return 'info';
      case 'LOW': return 'success';
      default: return 'secondary';
    }
  }

  exportExcel(type: 'issues' | 'complete' | 'gstr3b' = 'issues') {
    const id = this.run()?.runId;
    if (!id) return;

    this.exporting.set(true);
    this.auditApi.exportRun(id, type).pipe(
        finalize(() => this.exporting.set(false))
    ).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `Audit_Report_${this.run()?.ruleId}_${id.substring(0,8)}.xlsx`;
        a.click();
        URL.revokeObjectURL(url);
      },
      error: (err) => console.error('Export failed', err)
    });
  }
  
  formatDate(date: any): string {
    if (!date) return 'N/A';
    return new Intl.DateTimeFormat('en-IN', {
      day: '2-digit', month: 'short', year: 'numeric'
    }).format(new Date(date));
  }
}
