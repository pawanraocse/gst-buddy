import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideAnimations } from '@angular/platform-browser/animations';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { MessageService } from 'primeng/api';
import { AdminCreditsComponent } from './admin-credits.component';
import { AdminApiService, AdminDashboardStats, AdminUserSummary } from '../../core/services/admin-api.service';

describe('AdminCreditsComponent', () => {
  let component: AdminCreditsComponent;
  let fixture: ComponentFixture<AdminCreditsComponent>;
  let adminApiSpy: jasmine.SpyObj<AdminApiService>;
  let messageAddSpy: jasmine.Spy;
  let routerSpy: jasmine.SpyObj<Router>;

  const mockStats: AdminDashboardStats = {
    totalUsers: 20, activeUsers: 15, disabledUsers: 3, invitedUsers: 2,
    totalCreditsGranted: 1000, totalCreditsConsumed: 400,
    totalRevenueInr: 5000, activePlans: 2, totalTransactions: 50
  };

  const mockUsers: AdminUserSummary[] = [
    {
      userId: 'u1', email: 'alice@test.com', name: 'Alice', status: 'ACTIVE',
      source: 'COGNITO', tenantId: 'default', roles: ['admin'],
      creditRemaining: 70, createdAt: '', lastLoginAt: ''
    },
    {
      userId: 'u2', email: 'bob@test.com', name: 'Bob', status: 'ACTIVE',
      source: 'COGNITO', tenantId: 'default', roles: [],
      creditRemaining: 0, createdAt: '', lastLoginAt: ''
    }
  ];

  beforeEach(async () => {
    adminApiSpy = jasmine.createSpyObj('AdminApiService', ['getDashboardStats', 'getUsers']);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    adminApiSpy.getDashboardStats.and.returnValue(of(mockStats));
    adminApiSpy.getUsers.and.returnValue(of(mockUsers));

    await TestBed.configureTestingModule({
      imports: [AdminCreditsComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideAnimations(),
        { provide: AdminApiService, useValue: adminApiSpy },
        { provide: Router, useValue: routerSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AdminCreditsComponent);
    component = fixture.componentInstance;

    const msgService = fixture.debugElement.injector.get(MessageService);
    messageAddSpy = spyOn(msgService, 'add');

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load stats and users on init', () => {
    expect(adminApiSpy.getDashboardStats).toHaveBeenCalled();
    expect(adminApiSpy.getUsers).toHaveBeenCalled();
    expect(component.stats()).toEqual(mockStats);
    expect(component.allUsers().length).toBe(2);
    expect(component.loading()).toBeFalse();
  });

  it('should filter users by search query', () => {
    component.search = 'alice';
    component.filterUsers();
    expect(component.displayUsers().length).toBe(1);
    expect(component.displayUsers()[0].email).toBe('alice@test.com');
  });

  it('should reset to all users when search is cleared', () => {
    component.search = 'alice';
    component.filterUsers();
    expect(component.displayUsers().length).toBe(1);

    component.search = '';
    component.filterUsers();
    expect(component.displayUsers().length).toBe(2);
  });

  it('should format numbers in Indian locale', () => {
    expect(component.formatNumber(100000)).toBe('1,00,000');
  });

  it('should return correct status severity', () => {
    expect(component.statusSev('ACTIVE')).toBe('success');
    expect(component.statusSev('DISABLED')).toBe('danger');
    expect(component.statusSev('INVITED')).toBe('info');
  });

  it('should handle API error gracefully', () => {
    adminApiSpy.getDashboardStats.and.returnValue(throwError(() => new Error('fail')));
    adminApiSpy.getUsers.and.returnValue(throwError(() => new Error('fail')));

    component.loading.set(true);
    component.ngOnInit();

    expect(component.loading()).toBeFalse();
    expect(messageAddSpy).toHaveBeenCalledWith(
      jasmine.objectContaining({ severity: 'error' })
    );
  });
});
