import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { InputTextModule } from 'primeng/inputtext';
import { InputNumberModule } from 'primeng/inputnumber';
import { DialogModule } from 'primeng/dialog';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import {
  AdminApiService, AdminPlan, CreatePlanRequest, UpdatePlanRequest
} from '../../core/services/admin-api.service';

@Component({
  selector: 'app-admin-plans',
  standalone: true,
  imports: [
    CommonModule, FormsModule, ButtonModule, TagModule, InputTextModule,
    InputNumberModule, DialogModule, ToggleSwitchModule, ToastModule
  ],
  providers: [MessageService],
  template: `
    <div class="admin-plans">
      <p-toast></p-toast>

      <div class="page-header">
        <div>
          <h1>Plan Management</h1>
          <p class="text-secondary">Create, edit, and toggle pricing plans available on the platform.</p>
        </div>
        <button pButton icon="pi pi-plus" label="New Plan" (click)="openCreateDialog()"></button>
      </div>

      @if (loading()) {
        <div class="loading-state"><i class="pi pi-spin pi-spinner" style="font-size: 2rem"></i></div>
      } @else {
        <div class="plans-grid">
          @for (plan of plans(); track plan.id) {
            <div class="plan-card" [class.inactive]="!plan.isActive">
              <div class="plan-header">
                <div class="plan-name-row">
                  <h3>{{ plan.displayName }}</h3>
                  <p-tag [value]="plan.isActive ? 'Active' : 'Inactive'"
                         [severity]="plan.isActive ? 'success' : 'danger'" />
                </div>
                <span class="plan-slug">{{ plan.name }}</span>
              </div>

              <div class="plan-price">
                <span class="currency">&#8377;</span>
                <span class="amount">{{ plan.priceInr | number:'1.0-0' }}</span>
              </div>

              <div class="plan-details">
                <div class="detail-row">
                  <i class="pi pi-bolt"></i>
                  <span>{{ plan.credits }} credits</span>
                </div>
                @if (plan.isTrial) {
                  <div class="detail-row trial">
                    <i class="pi pi-gift"></i>
                    <span>Trial plan</span>
                  </div>
                }
                @if (plan.validityDays) {
                  <div class="detail-row">
                    <i class="pi pi-calendar"></i>
                    <span>{{ plan.validityDays }} days validity</span>
                  </div>
                }
                @if (plan.description) {
                  <p class="plan-desc">{{ plan.description }}</p>
                }
              </div>

              <div class="plan-actions">
                <button pButton icon="pi pi-pencil" label="Edit" [outlined]="true"
                        (click)="openEditDialog(plan)"></button>
                <button pButton [icon]="plan.isActive ? 'pi pi-eye-slash' : 'pi pi-eye'"
                        [label]="plan.isActive ? 'Deactivate' : 'Activate'"
                        [severity]="plan.isActive ? 'warn' : 'success'" [outlined]="true"
                        (click)="togglePlan(plan)"></button>
              </div>
            </div>
          }
          @if (plans().length === 0) {
            <div class="empty-state">
              <i class="pi pi-credit-card" style="font-size: 2.5rem; opacity: 0.3"></i>
              <p>No plans created yet.</p>
              <button pButton label="Create First Plan" icon="pi pi-plus"
                      (click)="openCreateDialog()"></button>
            </div>
          }
        </div>
      }

      <!-- Create / Edit Plan Dialog -->
      <p-dialog [header]="editingPlan ? 'Edit Plan' : 'Create Plan'"
                [(visible)]="showDialog" [modal]="true" [style]="{width: '480px'}" [closable]="true">
        <div class="flex flex-column gap-3 pt-2">
          @if (!editingPlan) {
            <div class="flex flex-column gap-1">
              <label class="font-medium">Slug (unique name)</label>
              <input pInputText [(ngModel)]="form.name" placeholder="e.g. starter-50" class="w-full" />
            </div>
          }
          <div class="flex flex-column gap-1">
            <label class="font-medium">Display Name</label>
            <input pInputText [(ngModel)]="form.displayName" placeholder="e.g. Starter Pack" class="w-full" />
          </div>
          <div class="grid">
            <div class="col-6">
              <label class="font-medium">Price (INR)</label>
              <p-inputNumber [(ngModel)]="form.priceInr" [min]="0" mode="currency" currency="INR"
                             locale="en-IN" styleClass="w-full"></p-inputNumber>
            </div>
            <div class="col-6">
              <label class="font-medium">Credits</label>
              <p-inputNumber [(ngModel)]="form.credits" [min]="1" [showButtons]="true"
                             styleClass="w-full"></p-inputNumber>
            </div>
          </div>
          <div class="grid">
            <div class="col-6">
              <label class="font-medium">Validity (days)</label>
              <p-inputNumber [(ngModel)]="form.validityDays" [min]="1" [showButtons]="true"
                             styleClass="w-full"></p-inputNumber>
            </div>
            <div class="col-6">
              <label class="font-medium">Sort Order</label>
              <p-inputNumber [(ngModel)]="form.sortOrder" [min]="0" [showButtons]="true"
                             styleClass="w-full"></p-inputNumber>
            </div>
          </div>
          <div class="flex align-items-center gap-2">
            <p-toggleSwitch [(ngModel)]="form.isTrial"></p-toggleSwitch>
            <label>Trial plan (one per user)</label>
          </div>
          <div class="flex flex-column gap-1">
            <label class="font-medium">Description</label>
            <input pInputText [(ngModel)]="form.description" placeholder="Optional description" class="w-full" />
          </div>
          <div class="flex justify-content-end gap-2 mt-2">
            <button pButton label="Cancel" [text]="true" (click)="showDialog = false"></button>
            <button pButton [label]="editingPlan ? 'Save' : 'Create'" icon="pi pi-check"
                    [disabled]="!isFormValid()" (click)="savePlan()"></button>
          </div>
        </div>
      </p-dialog>
    </div>
  `,
  styles: [`
    .admin-plans { padding: 0.5rem; }
    .page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 1.5rem; flex-wrap: wrap; gap: 1rem; }
    .page-header h1 { font-size: 1.75rem; font-weight: 700; margin: 0 0 0.25rem; }
    .page-header p { margin: 0; font-size: 0.95rem; }
    .loading-state { display: flex; justify-content: center; padding: 4rem; }

    .plans-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
      gap: 1.25rem;
    }

    .plan-card {
      background: rgba(255,255,255,0.85); backdrop-filter: blur(12px);
      border-radius: 20px; padding: 1.5rem; border: 1px solid rgba(255,255,255,0.6);
      box-shadow: 0 4px 24px -4px rgba(0,0,0,0.06);
      transition: transform 0.2s, box-shadow 0.2s;
      &:hover { transform: translateY(-2px); box-shadow: 0 8px 32px -4px rgba(0,0,0,0.1); }
      &.inactive { opacity: 0.65; }
    }
    .plan-header { margin-bottom: 1rem; }
    .plan-name-row { display: flex; justify-content: space-between; align-items: center; }
    .plan-name-row h3 { margin: 0; font-size: 1.15rem; font-weight: 600; }
    .plan-slug { font-size: 0.75rem; color: var(--text-secondary); font-family: monospace; }

    .plan-price { margin-bottom: 1rem; }
    .currency { font-size: 1.1rem; font-weight: 500; vertical-align: top; }
    .amount { font-size: 2rem; font-weight: 700; }

    .plan-details { display: flex; flex-direction: column; gap: 0.5rem; margin-bottom: 1.25rem; }
    .detail-row { display: flex; align-items: center; gap: 0.5rem; font-size: 0.9rem; color: var(--text-secondary); }
    .detail-row.trial { color: #d97706; }
    .plan-desc { font-size: 0.85rem; color: var(--text-secondary); margin: 0.25rem 0 0; }

    .plan-actions { display: flex; gap: 0.5rem; }

    .empty-state {
      grid-column: 1 / -1; display: flex; flex-direction: column;
      align-items: center; gap: 1rem; padding: 3rem; color: var(--text-secondary);
    }
  `]
})
export class AdminPlansComponent implements OnInit {
  private readonly adminApi = inject(AdminApiService);
  private readonly messageService = inject(MessageService);

  loading = signal(true);
  plans = signal<AdminPlan[]>([]);
  showDialog = false;
  editingPlan: AdminPlan | null = null;

  form: {
    name: string; displayName: string; priceInr: number; credits: number;
    isTrial: boolean; description: string; validityDays: number | null; sortOrder: number;
  } = this.emptyForm();

  ngOnInit(): void {
    this.loadPlans();
  }

  loadPlans(): void {
    this.loading.set(true);
    this.adminApi.getAllPlans().subscribe({
      next: data => { this.plans.set(data); this.loading.set(false); },
      error: () => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to load plans' });
        this.loading.set(false);
      }
    });
  }

  openCreateDialog(): void {
    this.editingPlan = null;
    this.form = this.emptyForm();
    this.showDialog = true;
  }

  openEditDialog(plan: AdminPlan): void {
    this.editingPlan = plan;
    this.form = {
      name: plan.name,
      displayName: plan.displayName,
      priceInr: plan.priceInr,
      credits: plan.credits,
      isTrial: plan.isTrial,
      description: plan.description || '',
      validityDays: plan.validityDays,
      sortOrder: plan.sortOrder || 0
    };
    this.showDialog = true;
  }

  savePlan(): void {
    if (this.editingPlan) {
      const req: UpdatePlanRequest = {
        displayName: this.form.displayName,
        priceInr: this.form.priceInr,
        credits: this.form.credits,
        isTrial: this.form.isTrial,
        description: this.form.description || undefined,
        validityDays: this.form.validityDays ?? undefined,
        sortOrder: this.form.sortOrder
      };
      this.adminApi.updatePlan(this.editingPlan.id, req).subscribe({
        next: () => {
          this.messageService.add({ severity: 'success', summary: 'Updated', detail: 'Plan updated successfully' });
          this.showDialog = false;
          this.loadPlans();
        },
        error: () => this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to update plan' })
      });
    } else {
      const req: CreatePlanRequest = {
        name: this.form.name,
        displayName: this.form.displayName,
        priceInr: this.form.priceInr,
        credits: this.form.credits,
        isTrial: this.form.isTrial,
        description: this.form.description || undefined,
        validityDays: this.form.validityDays ?? undefined,
        sortOrder: this.form.sortOrder
      };
      this.adminApi.createPlan(req).subscribe({
        next: () => {
          this.messageService.add({ severity: 'success', summary: 'Created', detail: 'Plan created successfully' });
          this.showDialog = false;
          this.loadPlans();
        },
        error: () => this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to create plan' })
      });
    }
  }

  togglePlan(plan: AdminPlan): void {
    this.adminApi.togglePlan(plan.id).subscribe({
      next: () => {
        const action = plan.isActive ? 'Deactivated' : 'Activated';
        this.messageService.add({ severity: 'success', summary: action, detail: `${plan.displayName} has been ${action.toLowerCase()}` });
        this.loadPlans();
      },
      error: () => this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to toggle plan' })
    });
  }

  isFormValid(): boolean {
    return !!(this.form.displayName && this.form.credits > 0 &&
      (this.editingPlan || this.form.name));
  }

  private emptyForm() {
    return { name: '', displayName: '', priceInr: 0, credits: 10, isTrial: false, description: '', validityDays: null as number | null, sortOrder: 0 };
  }
}
