import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';

const ADMIN_BASE = `${environment.apiUrl}/auth/api/v1/admin`;

// ── DTOs ──────────────────────────────────────────────────────

export interface AdminDashboardStats {
  totalUsers: number;
  activeUsers: number;
  disabledUsers: number;
  invitedUsers: number;
  totalCreditsGranted: number;
  totalCreditsConsumed: number;
  totalRevenueInr: number;
  activePlans: number;
  totalTransactions: number;
}

export interface AdminUserSummary {
  userId: string;
  email: string;
  name: string;
  status: string;
  source: string;
  tenantId: string;
  roles: string[];
  creditRemaining: number;
  createdAt: string;
  lastLoginAt: string;
}

export interface WalletSummary {
  total: number;
  used: number;
  remaining: number;
}

export interface AdminUserDetail {
  userId: string;
  email: string;
  name: string;
  avatarUrl: string;
  status: string;
  source: string;
  tenantId: string;
  roles: string[];
  wallet: WalletSummary;
  firstLoginAt: string;
  lastLoginAt: string;
  createdAt: string;
}

export interface AdminTransaction {
  id: number;
  userId: string;
  type: string;
  credits: number;
  balanceAfter: number;
  referenceType: string;
  referenceId: string;
  description: string;
  createdAt: string;
}

export interface AdminPlan {
  id: number;
  name: string;
  displayName: string;
  priceInr: number;
  credits: number;
  isTrial: boolean;
  isActive: boolean;
  description: string;
  validityDays: number;
  sortOrder: number;
}

export interface CreatePlanRequest {
  name: string;
  displayName: string;
  priceInr: number;
  credits: number;
  isTrial: boolean;
  description?: string;
  validityDays?: number;
  sortOrder?: number;
}

export interface UpdatePlanRequest {
  displayName?: string;
  priceInr?: number;
  credits?: number;
  isTrial?: boolean;
  isActive?: boolean;
  description?: string;
  validityDays?: number;
  sortOrder?: number;
}

export interface GrantCreditsRequest {
  credits: number;
  description?: string;
}

export interface RoleDto {
  id: string;
  name: string;
  description: string;
  scope: string;
}

// ── Service ───────────────────────────────────────────────────

@Injectable({ providedIn: 'root' })
export class AdminApiService {
  private readonly http = inject(HttpClient);

  // Dashboard
  getDashboardStats(): Observable<AdminDashboardStats> {
    return this.http.get<AdminDashboardStats>(`${ADMIN_BASE}/dashboard/stats`);
  }

  getRoles(): Observable<RoleDto[]> {
    return this.http.get<RoleDto[]>(`${ADMIN_BASE}/dashboard/roles`);
  }

  // Users — backend returns Spring Page<AdminUserDetailDto>, map to flat summary
  getUsers(params?: { status?: string; search?: string }): Observable<AdminUserSummary[]> {
    let httpParams = new HttpParams();
    httpParams = httpParams.set('size', '1000');
    if (params?.status) httpParams = httpParams.set('status', params.status);
    if (params?.search) httpParams = httpParams.set('search', params.search);
    return this.http
      .get<{ content: AdminUserDetail[] }>(`${ADMIN_BASE}/users`, { params: httpParams })
      .pipe(map(page => page.content.map(u => ({
        userId: u.userId,
        email: u.email,
        name: u.name,
        status: u.status,
        source: u.source,
        tenantId: u.tenantId,
        roles: u.roles?.length ? u.roles : ['user'],
        creditRemaining: u.wallet?.remaining ?? 0,
        createdAt: u.createdAt,
        lastLoginAt: u.lastLoginAt
      }))));
  }

  getUserDetail(userId: string): Observable<AdminUserDetail> {
    return this.http.get<AdminUserDetail>(`${ADMIN_BASE}/users/${userId}`).pipe(
      map(u => ({
        ...u,
        roles: u.roles?.length ? u.roles : ['user']
      }))
    );
  }

  enableUser(userId: string): Observable<void> {
    return this.http.post<void>(`${ADMIN_BASE}/users/${userId}/enable`, null);
  }

  suspendUser(userId: string): Observable<void> {
    return this.http.post<void>(`${ADMIN_BASE}/users/${userId}/suspend`, null);
  }

  deleteUser(userId: string): Observable<void> {
    return this.http.delete<void>(`${ADMIN_BASE}/users/${userId}`);
  }

  assignRole(userId: string, roleId: string): Observable<void> {
    return this.http.post<void>(`${ADMIN_BASE}/users/${userId}/roles`, { roleId });
  }

  removeRole(userId: string, roleId: string): Observable<void> {
    return this.http.delete<void>(`${ADMIN_BASE}/users/${userId}/roles/${roleId}`);
  }

  // Credits — backend routes are under /credits/wallets/{userId}
  getUserTransactions(userId: string): Observable<AdminTransaction[]> {
    return this.http.get<AdminTransaction[]>(`${ADMIN_BASE}/credits/wallets/${userId}/transactions`);
  }

  grantCredits(userId: string, request: GrantCreditsRequest): Observable<void> {
    return this.http.post<void>(`${ADMIN_BASE}/credits/wallets/${userId}/grant`, request);
  }

  revokeCredits(userId: string, request: GrantCreditsRequest): Observable<void> {
    return this.http.post<void>(`${ADMIN_BASE}/credits/wallets/${userId}/revoke`, request);
  }

  // Plans
  getAllPlans(): Observable<AdminPlan[]> {
    return this.http.get<AdminPlan[]>(`${ADMIN_BASE}/plans`);
  }

  createPlan(request: CreatePlanRequest): Observable<AdminPlan> {
    return this.http.post<AdminPlan>(`${ADMIN_BASE}/plans`, request);
  }

  updatePlan(planId: number, request: UpdatePlanRequest): Observable<AdminPlan> {
    return this.http.put<AdminPlan>(`${ADMIN_BASE}/plans/${planId}`, request);
  }

  togglePlan(planId: number): Observable<void> {
    return this.http.patch<void>(`${ADMIN_BASE}/plans/${planId}/toggle`, null);
  }
}
