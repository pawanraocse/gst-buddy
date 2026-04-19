import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  OnInit,
  signal
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { FindingSummaryDto, Gstr1UploadResult } from '../models/gstr1-late-fee.model';

@Component({
  selector: 'app-gstr1-results',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, ButtonModule, TableModule, TagModule, TooltipModule],
  templateUrl: './gstr1-results.component.html',
  styleUrls: ['./gstr1-results.component.scss']
})
export class Gstr1ResultsComponent implements OnInit {
  private readonly router = inject(Router);

  result   = signal<Gstr1UploadResult | null>(null);
  findings = computed(() => this.result()?.findingsSummary ?? []);
  isClean  = computed(() =>
    this.findings().every(f => f.severity === 'INFO' || f.impactAmount === 0)
  );

  totalImpact = computed(() =>
    this.findings().reduce((s, f) => s + (f.impactAmount ?? 0), 0)
  );

  ngOnInit() {
    const nav = this.router.getCurrentNavigation();
    const state = nav?.extras?.state as { result: Gstr1UploadResult } | undefined;
    if (state?.result) {
      this.result.set(state.result);
    } else {
      // Navigation without state (e.g. direct link) — redirect back to upload
      this.router.navigate(['/app/gstr1-late-fee']);
    }
  }

  // ── Helpers ───────────────────────────────────────────────────────────────
  severityTag(severity: string): 'danger' | 'warn' | 'success' | 'info' | 'secondary' {
    switch (severity) {
      case 'HIGH':   return 'danger';
      case 'MEDIUM': return 'warn';
      case 'LOW':    return 'info';
      case 'INFO':   return 'success';
      default:       return 'secondary';
    }
  }

  formatAmountInr(n: number): string {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    }).format(n);
  }

  goBack() {
    this.router.navigate(['/app/gstr1-late-fee']);
  }
}
