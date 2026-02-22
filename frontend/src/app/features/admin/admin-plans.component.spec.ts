import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { MessageService } from 'primeng/api';
import { AdminPlansComponent } from './admin-plans.component';
import { AdminApiService, AdminPlan } from '../../core/services/admin-api.service';

describe('AdminPlansComponent', () => {
  let component: AdminPlansComponent;
  let fixture: ComponentFixture<AdminPlansComponent>;
  let adminApiSpy: jasmine.SpyObj<AdminApiService>;
  let messageAddSpy: jasmine.Spy;

  const mockPlans: AdminPlan[] = [
    {
      id: 1, name: 'starter', displayName: 'Starter', priceInr: 99,
      credits: 10, isTrial: false, isActive: true, description: 'Entry plan',
      validityDays: 30, sortOrder: 1
    },
    {
      id: 2, name: 'trial', displayName: 'Free Trial', priceInr: 0,
      credits: 2, isTrial: true, isActive: true, description: 'Trial',
      validityDays: 7, sortOrder: 0
    }
  ];

  beforeEach(async () => {
    adminApiSpy = jasmine.createSpyObj('AdminApiService', [
      'getAllPlans', 'createPlan', 'updatePlan', 'togglePlan'
    ]);

    adminApiSpy.getAllPlans.and.returnValue(of(mockPlans));

    await TestBed.configureTestingModule({
      imports: [AdminPlansComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideAnimations(),
        { provide: AdminApiService, useValue: adminApiSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AdminPlansComponent);
    component = fixture.componentInstance;

    const msgService = fixture.debugElement.injector.get(MessageService);
    messageAddSpy = spyOn(msgService, 'add');

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load plans on init', () => {
    expect(adminApiSpy.getAllPlans).toHaveBeenCalled();
    expect(component.plans().length).toBe(2);
    expect(component.loading()).toBeFalse();
  });

  it('should handle load plans error', () => {
    adminApiSpy.getAllPlans.and.returnValue(throwError(() => new Error('fail')));
    component.loadPlans();
    expect(component.loading()).toBeFalse();
    expect(messageAddSpy).toHaveBeenCalledWith(
      jasmine.objectContaining({ severity: 'error' })
    );
  });

  it('should open create dialog with empty form', () => {
    component.openCreateDialog();
    expect(component.showDialog).toBeTrue();
    expect(component.editingPlan).toBeNull();
    expect(component.form.name).toBe('');
  });

  it('should open edit dialog with plan data', () => {
    component.openEditDialog(mockPlans[0]);
    expect(component.showDialog).toBeTrue();
    expect(component.editingPlan).toEqual(mockPlans[0]);
    expect(component.form.displayName).toBe('Starter');
    expect(component.form.credits).toBe(10);
  });

  it('should create a new plan', () => {
    adminApiSpy.createPlan.and.returnValue(of(mockPlans[0]));
    component.editingPlan = null;
    component.form = {
      name: 'pro', displayName: 'Pro', priceInr: 499, credits: 50,
      isTrial: false, description: '', validityDays: 90, sortOrder: 2
    };
    component.savePlan();
    expect(adminApiSpy.createPlan).toHaveBeenCalled();
    expect(component.showDialog).toBeFalse();
  });

  it('should update an existing plan', () => {
    adminApiSpy.updatePlan.and.returnValue(of(mockPlans[0]));
    component.editingPlan = mockPlans[0];
    component.form = {
      name: 'starter', displayName: 'Starter v2', priceInr: 149, credits: 15,
      isTrial: false, description: 'Updated', validityDays: 30, sortOrder: 1
    };
    component.savePlan();
    expect(adminApiSpy.updatePlan).toHaveBeenCalledWith(1, jasmine.objectContaining({
      displayName: 'Starter v2'
    }));
  });

  it('should toggle a plan', () => {
    adminApiSpy.togglePlan.and.returnValue(of(void 0));
    component.togglePlan(mockPlans[0]);
    expect(adminApiSpy.togglePlan).toHaveBeenCalledWith(1);
  });

  it('should validate form correctly', () => {
    component.editingPlan = null;
    component.form = { name: '', displayName: '', priceInr: 0, credits: 0, isTrial: false, description: '', validityDays: null, sortOrder: 0 };
    expect(component.isFormValid()).toBeFalse();

    component.form.name = 'test';
    component.form.displayName = 'Test';
    component.form.credits = 5;
    expect(component.isFormValid()).toBeTrue();
  });

  it('should skip name validation when editing', () => {
    component.editingPlan = mockPlans[0];
    component.form = { name: '', displayName: 'Test', priceInr: 0, credits: 5, isTrial: false, description: '', validityDays: null, sortOrder: 0 };
    expect(component.isFormValid()).toBeTrue();
  });
});
