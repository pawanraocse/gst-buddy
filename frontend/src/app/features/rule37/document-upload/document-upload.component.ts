import { Component, output, input, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { MessageService } from 'primeng/api';

@Component({
  selector: 'app-document-upload',
  standalone: true,
  imports: [CommonModule, FormsModule, ButtonModule],
  templateUrl: './document-upload.component.html',
  styleUrls: ['./document-upload.component.scss']
})
export class DocumentUploadComponent {
  private messageService = inject(MessageService);
  filesSelected = output<File[]>();
  disabled = input<boolean>(false);
  showSample = signal(false);

  private validateFiles(files: FileList | File[]): void {
    const validFiles: File[] = [];
    const invalidFiles: string[] = [];
    const allowed = ['.xlsx', '.xls'];

    Array.from(files).forEach(f => {
      const ext = f.name.substring(f.name.lastIndexOf('.')).toLowerCase();
      if (allowed.includes(ext)) {
        validFiles.push(f);
      } else {
        invalidFiles.push(f.name);
      }
    });

    if (invalidFiles.length > 0) {
      this.messageService.add({
        severity: 'error',
        summary: 'Invalid File Format',
        detail: `Unsupported files: ${invalidFiles.join(', ')}. Please use .xlsx or .xls.`
      });
    }

    if (validFiles.length > 0) {
      this.filesSelected.emit(validFiles);
    }
  }

  onFileChange(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.validateFiles(input.files);
    }
    input.value = '';
  }

  onDragOver(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    // Add visual cue for drag over if needed
  }

  onDragLeave(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
  }

  onDrop(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    if (this.disabled()) return;

    if (event.dataTransfer?.files && event.dataTransfer.files.length > 0) {
      this.validateFiles(event.dataTransfer.files);
    }
  }
}
