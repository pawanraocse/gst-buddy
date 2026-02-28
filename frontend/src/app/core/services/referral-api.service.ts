import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

const AUTH_BASE = `${environment.apiUrl}/auth`;

export interface ReferralCodeDto {
    referralCode: string;
    referralLink: string;
}

export interface ReferralStatsDto {
    referralCode: string;
    totalReferrals: number;
    totalCreditsEarned: number;
    rewardPerReferral: number;
}

/**
 * Referral System API Service — code generation and stats.
 */
@Injectable({ providedIn: 'root' })
export class ReferralApiService {
    private readonly http = inject(HttpClient);

    /**
     * Get or generate the current user's referral code and shareable link.
     */
    getReferralCode(): Observable<ReferralCodeDto> {
        return this.http.get<ReferralCodeDto>(`${AUTH_BASE}/api/v1/referral/code`);
    }

    /**
     * Get referral statistics for the current user.
     */
    getReferralStats(): Observable<ReferralStatsDto> {
        return this.http.get<ReferralStatsDto>(`${AUTH_BASE}/api/v1/referral/stats`);
    }
}
