import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { AuditApiService } from '../../../core/services/audit-api.service';
import { AuditRuleInfo, UploadResult } from '../../../shared/models/audit.model';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';
import { FileUploadModule } from 'primeng/fileupload';
import { MessageModule } from 'primeng/message';
import { StepperModule } from 'primeng/stepper';
import { DividerModule } from 'primeng/divider';
import { TooltipModule } from 'primeng/tooltip';
import { MessageService } from 'primeng/api';
import { finalize } from 'rxjs';

@Component({
  selector: 'app-audit-upload',
  standalone: true,
  imports: [
    CommonModule, 
    FormsModule, 
    RouterModule, 
    CardModule, 
    ButtonModule, 
    SelectModule, 
    FileUploadModule, 
    MessageModule, 
    StepperModule,
    DividerModule,
    CardModule,
    TooltipModule
  ],
  templateUrl: './audit-upload.component.html',
  styleUrls: ['./audit-upload.component.scss']
})
export class AuditUploadComponent implements OnInit {
  private auditApi = inject(AuditApiService);
  private messageService = inject(MessageService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  rules = signal<AuditRuleInfo[]>([]);
  selectedRule = signal<AuditRuleInfo | null>(null);
  asOnDate = signal<string>(new Date().toISOString().split('T')[0]);
  files = signal<File[]>([]);
  loading = signal(false);
  executing = signal(false);

  ngOnInit(): void {
    this.loadRules();
    
    // Check for ruleId in query params (passed from dashboard)
    this.route.queryParams.subscribe(params => {
      if (params['ruleId']) {
        const found = this.rules().find(r => r.ruleId === params['ruleId']);
        if (found) this.selectedRule.set(found);
      }
    });
  }

  loadRules() {
    this.loading.set(true);
    this.auditApi.getAvailableRules().pipe(
      finalize(() => this.loading.set(false))
    ).subscribe(res => {
      this.rules.set(res);
      // Auto-select first if none selected
      if (!this.selectedRule() && res.length > 0) {
        this.selectedRule.set(res[0]);
      }
    });
  }

  onFileSelect(event: any) {
    this.files.set(event.currentFiles);
  }

  onFileRemove(event: any) {
    this.files.update(f => f.filter(file => file !== event.file));
  }

  executeAudit() {
    if (!this.selectedRule() || this.files().length === 0) return;

    this.executing.set(true);
    this.auditApi.uploadLedgers(
      this.files(), 
      this.asOnDate(), 
      this.selectedRule()?.ruleId
    ).pipe(
      finalize(() => this.executing.set(false))
    ).subscribe({
      next: (res) => {
        this.messageService.add({ severity: 'success', summary: 'Audit Scheduled', detail: 'Processing your files...' });
        // Navigate to history or detail
        this.router.navigate(['/app/audit/history', res.stringRunId]);
      },
      error: (err) => {
        this.messageService.add({ 
            severity: 'error', 
            summary: 'Upload Failed', 
            detail: err.error?.message || 'Check your credits and file format.' 
        });
      }
    });
  }
}
