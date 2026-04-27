import { Component, Input, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FindingSummary } from '../../../shared/models/audit.model';
import { TableModule } from 'primeng/table';

@Component({
  selector: 'app-comprehensive-report',
  standalone: true,
  imports: [CommonModule, TableModule],
  templateUrl: './comprehensive-report.component.html',
  styleUrls: ['./comprehensive-report.component.scss']
})
export class ComprehensiveReportComponent {
  @Input() findings: FindingSummary[] = [];
  @Input() threeWayReconFindings: any;
  @Input() itcMismatches: any;
  @Input() rcmMismatches: any;

  formatCurrency(value: number): string {
    if (value === undefined || value === null) return '₹0';
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(value);
  }

  getThreeWayDeltas() {
    if (!this.threeWayReconFindings || !this.threeWayReconFindings.deltas) return [];
    return this.threeWayReconFindings.deltas;
  }

  getItcMismatches() {
    if (!this.itcMismatches || !this.itcMismatches.mismatches) return [];
    return this.itcMismatches.mismatches;
  }

  getRcmMismatches() {
    if (!this.rcmMismatches || !this.rcmMismatches.mismatches) return [];
    return this.rcmMismatches.mismatches;
  }
}
