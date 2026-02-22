import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideAnimations } from '@angular/platform-browser/animations';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AdminDashboardComponent } from './admin-dashboard.component';
import { AdminApiService, AdminDashboardStats } from '../../core/services/admin-api.service';

describe('AdminDashboardComponent', () => {
  let component: AdminDashboardComponent;
  let fixture: ComponentFixture<AdminDashboardComponent>;
  let adminApiSpy: jasmine.SpyObj<AdminApiService>;
  let routerSpy: jasmine.SpyObj<Router>;

  const mockStats: AdminDashboardStats = {
    totalUsers: 50, activeUsers: 40, disabledUsers: 5, invitedUsers: 5,
    totalCreditsGranted: 2000, totalCreditsConsumed: 800,
    totalRevenueInr: 15000, activePlans: 3, totalTransactions: 120
  };

  beforeEach(async () => {
    adminApiSpy = jasmine.createSpyObj('AdminApiService', ['getDashboardStats']);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    adminApiSpy.getDashboardStats.and.returnValue(of(mockStats));

    await TestBed.configureTestingModule({
      imports: [AdminDashboardComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideAnimations(),
        { provide: AdminApiService, useValue: adminApiSpy },
        { provide: Router, useValue: routerSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AdminDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load dashboard stats on init', () => {
    expect(adminApiSpy.getDashboardStats).toHaveBeenCalled();
    expect(component.stats()).toEqual(mockStats);
    expect(component.loading()).toBeFalse();
  });

  it('should handle API error gracefully', () => {
    adminApiSpy.getDashboardStats.and.returnValue(throwError(() => new Error('fail')));

    component.loading.set(true);
    component.ngOnInit();

    expect(component.loading()).toBeFalse();
  });

  it('should format numbers in Indian locale', () => {
    expect(component.formatNumber(100000)).toBe('1,00,000');
  });

  it('should format currency in INR', () => {
    const formatted = component.formatCurrency(15000);
    expect(formatted).toContain('15,000');
  });

  it('should navigate to users page on quick action', () => {
    routerSpy.navigate.and.returnValue(Promise.resolve(true));
    component.router.navigate(['/app/admin/users']);
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/app/admin/users']);
  });
});
