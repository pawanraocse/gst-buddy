import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { AuthService } from '../../core/auth.service';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { CreditApiService, WalletDto, PlanDto } from '../../core/services/credit-api.service';
import { ReferralApiService } from '../../core/services/referral-api.service';
import { SupportApiService, SupportTicketDto } from '../../core/services/support-api.service';

@Component({
  selector: 'app-account-settings',
  standalone: true,
  imports: [CommonModule, FormsModule, CardModule, ButtonModule, DialogModule, InputTextModule, ToastModule, ToggleSwitchModule],
  providers: [MessageService],
  templateUrl: './account-settings.component.html',
  styleUrls: ['./account-settings.component.scss']
})
export class AccountSettingsComponent implements OnInit {
  authService = inject(AuthService);
  private http = inject(HttpClient);
  private router = inject(Router);
  private messageService = inject(MessageService);
  private creditApi = inject(CreditApiService);
  private referralApi = inject(ReferralApiService);
  private supportApi = inject(SupportApiService);

  deleting = signal(false);
  showDeleteDialog = false;
  confirmationText = '';
  wallet = signal<WalletDto | null>(null);
  walletLoading = signal(true);

  // Tickets
  userTickets = signal<SupportTicketDto[]>([]);
  ticketsLoading = signal(false);

  // Plans
  plans = signal<PlanDto[]>([]);
  plansLoading = signal(false);
  showPlansDialog = false;

  // Mock preferences for UI
  preferences = {
    email_notifications: true,
    beta_features: false
  };

  // Referral
  referralCode = signal<string | null>(null);
  referralLink = signal<string | null>(null);
  referralCount = signal(0);
  referralCreditsEarned = signal(0);
  referralRewardPerReferral = signal(2);
  referralLoaded = signal(false);
  referralError = signal(false);
  linkCopied = signal(false);

  ngOnInit(): void {
    this.creditApi.getWallet().subscribe({
      next: (w) => { this.wallet.set(w); this.walletLoading.set(false); },
      error: () => { this.walletLoading.set(false); }
    });
    this.referralApi.getReferralCode().subscribe({
      next: (r) => {
        this.referralCode.set(r.referralCode);
        this.referralLink.set(r.referralLink);
        this.referralLoaded.set(true);
      },
      error: () => { this.referralError.set(true); }
    });
    this.referralApi.getReferralStats().subscribe({
      next: (s) => {
        this.referralCount.set(s.totalReferrals);
        this.referralCreditsEarned.set(s.totalCreditsEarned);
        this.referralRewardPerReferral.set(s.rewardPerReferral);
      },
      error: () => { }
    });
    this.loadUserTickets();
  }

  loadUserTickets() {
    this.ticketsLoading.set(true);
    this.supportApi.getMyTickets().subscribe({
      next: (t) => { this.userTickets.set(t); this.ticketsLoading.set(false); },
      error: () => { this.ticketsLoading.set(false); }
    });
  }

  get usagePercent(): number {
    const w = this.wallet();
    if (!w || w.total === 0) return 0;
    return Math.round((w.used / w.total) * 100);
  }

  openPlansDialog(): void {
    this.showPlansDialog = true;
    if (this.plans().length === 0) {
      this.plansLoading.set(true);
      this.creditApi.getPlans().subscribe({
        next: (p) => { 
          const nonTrialPlans = p.filter(plan => !plan.isTrial);
          this.plans.set(nonTrialPlans); 
          this.plansLoading.set(false); 
        },
        error: () => {
          this.plansLoading.set(false);
          this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to load plans.' });
        }
      });
    }
  }

  deleteAccount(): void {
    if (this.confirmationText !== 'DELETE') return;

    this.deleting.set(true);
    this.http.post(`${environment.apiUrl}/auth/api/v1/account/delete`, {
      confirmation: this.confirmationText
    }).subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: 'Account Deleted', detail: 'Redirecting...' });
        setTimeout(() => this.authService.logout(), 2000);
      },
      error: () => {
        this.deleting.set(false);
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to delete account.' });
      }
    });
  }

  copyReferralLink(): void {
    const link = this.referralLink();
    if (link) {
      navigator.clipboard.writeText(link).then(() => {
        this.linkCopied.set(true);
        this.messageService.add({ severity: 'success', summary: 'Copied!', detail: 'Referral link copied to clipboard', life: 2000 });
        setTimeout(() => this.linkCopied.set(false), 2000);
      });
    }
  }

  shareWhatsApp(): void {
    const link = this.referralLink();
    if (link) {
      const msg = encodeURIComponent(`Hey! Try Gstbuddies for Rule 37 compliance checks. Sign up with my link and we both get bonus credits: ${link}`);
      window.open(`https://wa.me/?text=${msg}`, '_blank');
    }
  }
}
