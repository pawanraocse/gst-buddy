import { Component, Input, OnChanges, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DialogModule } from 'primeng/dialog';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { SelectModule } from 'primeng/select';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { SupportApiService } from '../../../core/services/support-api.service';
import { MessageService } from 'primeng/api';

@Component({
  selector: 'app-support-ticket-dialog',
  standalone: true,
  imports: [
    CommonModule,
    DialogModule,
    ButtonModule,
    InputTextModule,
    TextareaModule,
    SelectModule,
    FormsModule,
    ReactiveFormsModule
  ],
  template: `
    <p-dialog
      [(visible)]="visible"
      [modal]="true"
      [draggable]="false"
      [resizable]="false"
      [style]="{ width: '520px', 'max-width': '95vw' }"
      [header]="submitted() ? '' : 'Submit a Support Request'"
      styleClass="support-dialog"
      (onHide)="onDialogHide()">

      <!-- ✅ Success State — only shown after submit -->
      @if (submitted()) {
        <div style="
          display: flex;
          flex-direction: column;
          align-items: center;
          text-align: center;
          padding: 2rem 1.5rem;
          gap: 1rem;
        ">
          <div style="
            width: 72px; height: 72px;
            border-radius: 50%;
            background: rgba(34,197,94,0.12);
            border: 2px solid rgba(34,197,94,0.35);
            display: flex; align-items: center; justify-content: center;
          ">
            <i class="pi pi-check" style="font-size: 2rem; color: #22c55e;"></i>
          </div>

          <h3 style="margin: 0; font-size: 1.25rem; font-weight: 700; color: var(--text-color, #1e293b);">
            Ticket Submitted!
          </h3>
          <p style="margin: 0; color: var(--text-secondary-color, #64748b); line-height: 1.6; font-size: 0.9rem;">
            Our team will get back to you soon. Use the reference below to track your request.
          </p>

          <div style="
            display: flex;
            align-items: center;
            gap: 0.75rem;
            padding: 0.75rem 1.5rem;
            background: var(--surface-ground, #f8fafc);
            border: 1px solid var(--surface-border, #e2e8f0);
            border-radius: 12px;
            margin-top: 0.25rem;
          ">
            <span style="font-size: 0.8rem; color: var(--text-secondary-color, #64748b); text-transform: uppercase; letter-spacing: 0.05em;">Ref ID</span>
            <span style="font-family: monospace; font-weight: 700; font-size: 1rem; color: var(--primary-color, #6366f1); letter-spacing: 0.05em;">
              #{{ createdTicketId() }}
            </span>
          </div>

          <button
            pButton
            label="Done"
            icon="pi pi-check"
            [outlined]="true"
            style="margin-top: 0.5rem;"
            (click)="visible = false">
          </button>
        </div>
      }

      <!-- 📝 Form State -->
      @if (!submitted()) {
        <form [formGroup]="supportForm" (ngSubmit)="submitTicket()" class="flex flex-column gap-3 py-2">

          <p style="color: var(--text-secondary-color, #64748b); margin: 0; font-size: 0.875rem; line-height: 1.6;">
            Need help or want to share feedback? Fill in the details below.
          </p>

          <!-- Category (mandatory) -->
          <div class="flex flex-column gap-1">
            <label class="font-semibold text-sm">Category <span style="color: #ef4444;">*</span></label>
            <p-select
              [options]="categories"
              formControlName="category"
              placeholder="Select a category"
              styleClass="w-full"
              appendTo="body">
            </p-select>
            @if (isInvalid('category')) {
              <small style="color: #ef4444;">Please select a category.</small>
            }
          </div>

          <!-- Email (only for anonymous users) -->
          @if (!isAuthenticated) {
            <div class="flex flex-column gap-1">
              <label class="font-semibold text-sm">Email Address <span style="color: #ef4444;">*</span></label>
              <input
                pInputText
                formControlName="email"
                placeholder="your@email.com"
                class="w-full"
              />
              @if (isInvalid('email')) {
                <small style="color: #ef4444;">Please enter a valid email address.</small>
              }
            </div>
          }

          <!-- Subject -->
          <div class="flex flex-column gap-1">
            <label class="font-semibold text-sm">Subject <span style="color: #ef4444;">*</span></label>
            <input
              pInputText
              formControlName="subject"
              placeholder="Brief summary of your request"
              class="w-full"
            />
            @if (isInvalid('subject')) {
              <small style="color: #ef4444;">Subject is required (min 5 characters).</small>
            }
          </div>

          <!-- Message -->
          <div class="flex flex-column gap-1">
            <label class="font-semibold text-sm">Message <span style="color: #ef4444;">*</span></label>
            <textarea
              pTextarea
              formControlName="description"
              rows="5"
              placeholder="Describe your issue or feedback in detail..."
              class="w-full"
              style="resize: none;">
            </textarea>
            @if (isInvalid('description')) {
              <small style="color: #ef4444;">Message is required (min 10 characters).</small>
            }
          </div>

          <!-- Actions -->
          <div class="flex justify-content-end gap-2 mt-1">
            <button
              pButton
              type="button"
              label="Cancel"
              [text]="true"
              (click)="visible = false"
              [disabled]="loading()">
            </button>
            <button
              pButton
              type="submit"
              label="Submit Ticket"
              icon="pi pi-send"
              [loading]="loading()"
              [disabled]="supportForm.invalid || loading()">
            </button>
          </div>
        </form>
      }
    </p-dialog>
  `
})
export class SupportTicketDialogComponent {
  @Input() isAuthenticated = false;

  visible = false;

  // Use signals — required for zoneless change detection
  loading = signal(false);
  submitted = signal(false);
  createdTicketId = signal<string>('');

  private fb = inject(FormBuilder);
  private supportApi = inject(SupportApiService);
  private messageService = inject(MessageService);

  categories = [
    { label: 'Technical Support', value: 'Technical Support' },
    { label: 'Billing & Subscriptions', value: 'Billing & Subscriptions' },
    { label: 'Feature Request', value: 'Feature Request' },
    { label: 'Bug Report', value: 'Bug Report' },
    { label: 'Other', value: 'Other' }
  ];

  supportForm = this.fb.group({
    category: ['', Validators.required],
    email: [''],
    subject: ['', [Validators.required, Validators.minLength(5)]],
    description: ['', [Validators.required, Validators.minLength(10)]]
  });

  show() {
    this.resetState();
    // Email required only for guest users
    if (!this.isAuthenticated) {
      this.supportForm.get('email')?.setValidators([Validators.required, Validators.email]);
    } else {
      this.supportForm.get('email')?.clearValidators();
    }
    this.supportForm.get('email')?.updateValueAndValidity();
    this.visible = true;
  }

  isInvalid(field: string): boolean {
    const ctrl = this.supportForm.get(field);
    return !!(ctrl && ctrl.invalid && ctrl.touched);
  }

  onDialogHide() {
    if (!this.submitted()) {
      this.resetState();
    }
  }

  private resetState() {
    this.supportForm.reset();
    this.loading.set(false);
    this.submitted.set(false);
    this.createdTicketId.set('');
  }

  submitTicket() {
    this.supportForm.markAllAsTouched();
    if (this.supportForm.invalid) return;

    this.loading.set(true);
    const formData = this.supportForm.value as any;

    const request$ = this.isAuthenticated
      ? this.supportApi.createTicket(formData)
      : this.supportApi.createPublicTicket(formData);

    request$.subscribe({
      next: (ticket) => {
        this.loading.set(false);
        this.createdTicketId.set(String(ticket.id).slice(0, 8).toUpperCase());
        this.submitted.set(true);
      },
      error: () => {
        this.loading.set(false);
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'Could not submit your ticket. Please try again.'
        });
      }
    });
  }
}
