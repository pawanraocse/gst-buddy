import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import {
  AdminApiService, AdminDashboardStats, AdminUserSummary,
  AdminUserDetail, AdminTransaction, AdminPlan, RoleDto
} from './admin-api.service';
import { environment } from '../../../environments/environment';

describe('AdminApiService', () => {
  let service: AdminApiService;
  let httpTesting: HttpTestingController;
  const BASE = `${environment.apiUrl}/auth/api/v1/admin`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(),
        provideHttpClientTesting(),
        AdminApiService
      ]
    });

    service = TestBed.inject(AdminApiService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  // ── Dashboard ──

  it('should fetch dashboard stats', () => {
    const mockStats: AdminDashboardStats = {
      totalUsers: 100, activeUsers: 80, disabledUsers: 5, invitedUsers: 15,
      totalCreditsGranted: 5000, totalCreditsConsumed: 3000,
      totalRevenueInr: 25000, activePlans: 3, totalTransactions: 200
    };

    service.getDashboardStats().subscribe(stats => {
      expect(stats.totalUsers).toBe(100);
      expect(stats.activeUsers).toBe(80);
    });

    const req = httpTesting.expectOne(`${BASE}/dashboard/stats`);
    expect(req.request.method).toBe('GET');
    req.flush(mockStats);
  });

  it('should fetch roles', () => {
    const mockRoles: RoleDto[] = [
      { id: 'admin', name: 'Admin', description: 'Admin role', scope: 'GLOBAL' }
    ];

    service.getRoles().subscribe(roles => {
      expect(roles.length).toBe(1);
      expect(roles[0].id).toBe('admin');
    });

    const req = httpTesting.expectOne(`${BASE}/dashboard/roles`);
    expect(req.request.method).toBe('GET');
    req.flush(mockRoles);
  });

  // ── Users ──

  it('should fetch users with optional filters', () => {
    service.getUsers({ status: 'ACTIVE', search: 'test' }).subscribe();

    const req = httpTesting.expectOne(r =>
      r.url === `${BASE}/users` &&
      r.params.get('status') === 'ACTIVE' &&
      r.params.get('search') === 'test'
    );
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should fetch users without filters', () => {
    service.getUsers().subscribe();

    const req = httpTesting.expectOne(`${BASE}/users`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should fetch user detail', () => {
    const mockDetail: AdminUserDetail = {
      userId: 'u1', email: 'a@b.com', name: 'Test', avatarUrl: '',
      status: 'ACTIVE', source: 'COGNITO', tenantId: 'default',
      roles: ['admin'], wallet: { total: 10, used: 3, remaining: 7 },
      firstLoginAt: '', lastLoginAt: '', createdAt: ''
    };

    service.getUserDetail('u1').subscribe(user => {
      expect(user.email).toBe('a@b.com');
      expect(user.roles).toContain('admin');
    });

    const req = httpTesting.expectOne(`${BASE}/users/u1`);
    expect(req.request.method).toBe('GET');
    req.flush(mockDetail);
  });

  it('should enable a user', () => {
    service.enableUser('u1').subscribe();
    const req = httpTesting.expectOne(`${BASE}/users/u1/enable`);
    expect(req.request.method).toBe('POST');
    req.flush(null);
  });

  it('should suspend a user', () => {
    service.suspendUser('u1').subscribe();
    const req = httpTesting.expectOne(`${BASE}/users/u1/suspend`);
    expect(req.request.method).toBe('POST');
    req.flush(null);
  });

  it('should delete a user', () => {
    service.deleteUser('u1').subscribe();
    const req = httpTesting.expectOne(`${BASE}/users/u1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('should assign a role', () => {
    service.assignRole('u1', 'admin').subscribe();
    const req = httpTesting.expectOne(`${BASE}/users/u1/roles`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ roleId: 'admin' });
    req.flush(null);
  });

  it('should remove a role', () => {
    service.removeRole('u1', 'admin').subscribe();
    const req = httpTesting.expectOne(`${BASE}/users/u1/roles/admin`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  // ── Credits ──

  it('should fetch user transactions', () => {
    service.getUserTransactions('u1').subscribe();
    const req = httpTesting.expectOne(`${BASE}/credits/u1/transactions`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should grant credits', () => {
    service.grantCredits('u1', { credits: 50, description: 'bonus' }).subscribe();
    const req = httpTesting.expectOne(`${BASE}/credits/u1/grant`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ credits: 50, description: 'bonus' });
    req.flush(null);
  });

  it('should revoke credits', () => {
    service.revokeCredits('u1', { credits: 10 }).subscribe();
    const req = httpTesting.expectOne(`${BASE}/credits/u1/revoke`);
    expect(req.request.method).toBe('POST');
    req.flush(null);
  });

  // ── Plans ──

  it('should fetch all plans', () => {
    service.getAllPlans().subscribe();
    const req = httpTesting.expectOne(`${BASE}/plans`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should create a plan', () => {
    const newPlan = { name: 'starter', displayName: 'Starter', priceInr: 99, credits: 10, isTrial: false };
    service.createPlan(newPlan).subscribe();
    const req = httpTesting.expectOne(`${BASE}/plans`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.name).toBe('starter');
    req.flush({ id: 1, ...newPlan });
  });

  it('should update a plan', () => {
    service.updatePlan(1, { displayName: 'Starter v2' }).subscribe();
    const req = httpTesting.expectOne(`${BASE}/plans/1`);
    expect(req.request.method).toBe('PUT');
    req.flush({});
  });

  it('should toggle a plan', () => {
    service.togglePlan(1).subscribe();
    const req = httpTesting.expectOne(`${BASE}/plans/1/toggle`);
    expect(req.request.method).toBe('PATCH');
    req.flush(null);
  });
});
