import {
  ChangeDetectionStrategy,
  Component,
  inject,
  signal
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { MessageService } from 'primeng/api';
import { Gstr1ApiService } from '../services/gstr1-api.service';
import { Gstr1UploadResult } from '../models/gstr1-late-fee.model';

type UploadState = 'idle' | 'parsing' | 'auditing' | 'done' | 'error';
type ConfidenceLevel = 'auto' | 'manual';

@Component({
  selector: 'app-gstr1-upload',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    ButtonModule, ToggleSwitchModule, TagModule, TooltipModule, ProgressSpinnerModule
  ],
  templateUrl: './gstr1-upload.component.html',
  styleUrls: ['./gstr1-upload.component.scss']
})
export class Gstr1UploadComponent {
  private readonly api    = inject(Gstr1ApiService);
  private readonly msgs   = inject(MessageService);
  private readonly router = inject(Router);

  // ── Upload state ──────────────────────────────────────────────────────────
  uploadState   = signal<UploadState>('idle');
  isDragOver    = signal(false);
  selectedFile  = signal<File | null>(null);

  // ── CA-controlled toggles ─────────────────────────────────────────────────
  isQrmp        = signal(false);
  isNilReturn   = signal(false);
  asOnDate      = signal(new Date().toISOString().slice(0, 10));

  // ── Toggle confidence (auto-detected vs manual) ───────────────────────────
  qrmpConfidence      = signal<ConfidenceLevel>('manual');
  nilReturnConfidence = signal<ConfidenceLevel>('manual');

  // ── Result (passed to results view via router state) ─────────────────────
  result = signal<Gstr1UploadResult | null>(null);

  readonly ALLOWED_TYPES = ['.pdf', '.json'];

  // ── File Drop Zone ────────────────────────────────────────────────────────
  onDragOver(e: DragEvent)  { e.preventDefault(); this.isDragOver.set(true); }
  onDragLeave(e: DragEvent) { e.preventDefault(); this.isDragOver.set(false); }

  onDrop(e: DragEvent) {
    e.preventDefault();
    this.isDragOver.set(false);
    const file = e.dataTransfer?.files?.[0];
    if (file) this.selectFile(file);
  }

  onFileChange(e: Event) {
    const input = e.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file) this.selectFile(file);
    input.value = '';
  }

  private selectFile(file: File) {
    const name = file.name.toLowerCase();
    if (!this.ALLOWED_TYPES.some(ext => name.endsWith(ext))) {
      this.msgs.add({
        severity: 'error', summary: 'Invalid File',
        detail: 'Only .pdf and .json files are accepted for GSTR-1 uploads.'
      });
      return;
    }
    this.selectedFile.set(file);
    this.uploadState.set('idle');
    this.result.set(null);
  }

  removeFile() {
    this.selectedFile.set(null);
    this.uploadState.set('idle');
    this.result.set(null);
  }

  // ── Submit ────────────────────────────────────────────────────────────────
  runAudit() {
    const file = this.selectedFile();
    if (!file) return;

    this.uploadState.set('parsing');

    this.api.uploadGstr1(
      file,
      this.isQrmp(),
      this.isNilReturn(),
      this.asOnDate()
    ).subscribe({
      next: (res) => {
        this.uploadState.set('done');
        this.result.set(res);
        this.router.navigate(['/app/gstr1-late-fee/results'], {
          state: { result: res }
        });
      },
      error: (err) => {
        this.uploadState.set('error');
        this.msgs.add({
          severity: 'error', summary: 'Audit Failed',
          detail: err?.error?.message ?? 'An unexpected error occurred. Please try again.'
        });
      }
    });
  }

  // ── Helpers ───────────────────────────────────────────────────────────────
  get stateLabel(): string {
    switch (this.uploadState()) {
      case 'parsing':  return 'Parsing document…';
      case 'auditing': return 'Running audit…';
      default: return 'Run Audit';
    }
  }

  get isLoading(): boolean {
    return this.uploadState() === 'parsing' || this.uploadState() === 'auditing';
  }

  formatBytes(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }
}
