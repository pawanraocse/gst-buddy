import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

const AUTH_BASE = `${environment.apiUrl}/auth`;

export interface PlanDto {
  id: number;
  name: string;
  displayName: string;
  priceInr: number;
  credits: number;
  isTrial: boolean;
  description: string;
}

export interface WalletDto {
  total: number;
  used: number;
  remaining: number;
}

/**
 * Credit System API Service — Plans, wallet balance, and credit operations.
 */
@Injectable({ providedIn: 'root' })
export class CreditApiService {
  private readonly http = inject(HttpClient);

  /**
   * Fetch all active pricing plans (public, no auth required).
   */
  getPlans(): Observable<PlanDto[]> {
    return this.http.get<PlanDto[]>(`${AUTH_BASE}/api/v1/plans`);
  }

  /**
   * Get current user's credit wallet balance (requires auth).
   */
  getWallet(): Observable<WalletDto> {
    return this.http.get<WalletDto>(`${AUTH_BASE}/api/v1/credits`);
  }
}
