import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { MessageService } from 'primeng/api';
import { ReferralApiService, ReferralCodeDto, ReferralStatsDto } from '../../core/services/referral-api.service';

@Component({
    selector: 'app-referral',
    standalone: true,
    imports: [CommonModule, CardModule, ButtonModule, TooltipModule, InputTextModule, MessageModule],
    template: `
    <div class="referral-container grid p-4 md:p-6 fadein animation-duration-500">

      <!-- Header -->
      <div class="col-12 mb-4">
        <h1 class="text-3xl font-bold text-900 m-0 tracking-tight">
          <i class="pi pi-users mr-2 text-primary"></i>Refer & Earn
        </h1>
        <p class="text-secondary mt-2 text-lg m-0">
          Invite friends to Gstbuddies and both of you earn bonus credits!
        </p>
      </div>

      <!-- Referral Code Card -->
      <div class="col-12 md:col-6 mb-4">
        <div class="surface-card border-1 surface-border border-round-xl p-5 shadow-1 h-full">
          <div class="flex align-items-center gap-3 mb-4">
            <div class="flex align-items-center justify-content-center border-round-lg"
                 style="width: 48px; height: 48px; background: linear-gradient(135deg, #10b981, #059669);">
              <i class="pi pi-link text-white text-xl"></i>
            </div>
            <div>
              <h3 class="text-lg font-bold text-900 m-0">Your Referral Code</h3>
              <p class="text-sm text-600 m-0 mt-1">Share this with friends & family</p>
            </div>
          </div>

          @if (loading()) {
          <div class="flex align-items-center justify-content-center py-4">
            <i class="pi pi-spin pi-spinner text-2xl text-primary"></i>
          </div>
          } @else if (referralCode()) {
          <!-- Code Display -->
          <div class="surface-100 border-round-lg p-4 mb-4 text-center">
            <code class="text-2xl font-bold text-900 letter-spacing-2">{{ referralCode() }}</code>
          </div>

          <!-- Shareable Link -->
          <div class="flex align-items-center gap-2 mb-4">
            <input pInputText [value]="referralLink() || ''" class="w-full text-sm" readonly />
            <button pButton type="button"
                    [icon]="linkCopied() ? 'pi pi-check' : 'pi pi-copy'"
                    [class]="linkCopied() ? 'p-button-success p-button-outlined' : 'p-button-primary p-button-outlined'"
                    pTooltip="Copy link"
                    (click)="copyLink()">
            </button>
          </div>

          <!-- Share Actions -->
          <div class="flex gap-2 flex-wrap">
            <button pButton type="button" icon="pi pi-copy" label="Copy Link"
                    [class]="linkCopied() ? 'p-button-success' : 'p-button-primary'"
                    (click)="copyLink()">
            </button>
            <button pButton type="button" icon="pi pi-whatsapp" label="WhatsApp"
                    class="p-button-success"
                    (click)="shareWhatsApp()">
            </button>
            <button pButton type="button" icon="pi pi-envelope" label="Email"
                    class="p-button-outlined"
                    (click)="shareEmail()">
            </button>
          </div>
          } @else {
          <p-message severity="warn" text="Unable to load referral code. Please try again later."
                     styleClass="w-full"></p-message>
          }
        </div>
      </div>

      <!-- Stats Card -->
      <div class="col-12 md:col-6 mb-4">
        <div class="surface-card border-1 surface-border border-round-xl p-5 shadow-1 h-full">
          <div class="flex align-items-center gap-3 mb-4">
            <div class="flex align-items-center justify-content-center border-round-lg"
                 style="width: 48px; height: 48px; background: linear-gradient(135deg, #8b5cf6, #6d28d9);">
              <i class="pi pi-chart-bar text-white text-xl"></i>
            </div>
            <div>
              <h3 class="text-lg font-bold text-900 m-0">Your Stats</h3>
              <p class="text-sm text-600 m-0 mt-1">Track your referral impact</p>
            </div>
          </div>

          <div class="grid">
            <div class="col-6 text-center">
              <div class="surface-100 border-round-lg p-4">
                <div class="text-3xl font-bold" style="color: #10b981;">{{ referralCount() }}</div>
                <div class="text-sm text-600 mt-2 font-medium">Friends Referred</div>
              </div>
            </div>
            <div class="col-6 text-center">
              <div class="surface-100 border-round-lg p-4">
                <div class="text-3xl font-bold" style="color: #8b5cf6;">+{{ creditsEarned() }}</div>
                <div class="text-sm text-600 mt-2 font-medium">Credits Earned</div>
              </div>
            </div>
          </div>

          <!-- How it works -->
          <div class="mt-4 pt-4 border-top-1 surface-border">
            <h4 class="text-sm font-bold text-700 mb-3 m-0">How it works</h4>
            <div class="flex flex-column gap-3">
              <div class="flex align-items-center gap-3">
                <div class="flex align-items-center justify-content-center border-circle surface-200"
                     style="min-width: 32px; height: 32px;">
                  <span class="text-sm font-bold text-700">1</span>
                </div>
                <span class="text-sm text-700">Share your referral link with friends</span>
              </div>
              <div class="flex align-items-center gap-3">
                <div class="flex align-items-center justify-content-center border-circle surface-200"
                     style="min-width: 32px; height: 32px;">
                  <span class="text-sm font-bold text-700">2</span>
                </div>
                <span class="text-sm text-700">They sign up using your link</span>
              </div>
              <div class="flex align-items-center gap-3">
                <div class="flex align-items-center justify-content-center border-circle surface-200"
                     style="min-width: 32px; height: 32px;">
                  <span class="text-sm font-bold text-700">3</span>
                </div>
                <span class="text-sm text-700">Both of you get {{ rewardPerReferral() }} bonus credits!</span>
              </div>
            </div>
          </div>
        </div>
      </div>

    </div>
  `,
    styles: [`
    .letter-spacing-2 { letter-spacing: 0.15em; }
  `]
})
export class ReferralComponent implements OnInit {
    private referralApi = inject(ReferralApiService);
    private messageService = inject(MessageService);

    loading = signal(true);
    referralCode = signal<string | null>(null);
    referralLink = signal<string | null>(null);
    referralCount = signal(0);
    creditsEarned = signal(0);
    rewardPerReferral = signal(2);
    linkCopied = signal(false);

    ngOnInit(): void {
        this.referralApi.getReferralCode().subscribe({
            next: (r) => {
                this.referralCode.set(r.referralCode);
                this.referralLink.set(r.referralLink);
                this.loading.set(false);
            },
            error: () => this.loading.set(false)
        });
        this.referralApi.getReferralStats().subscribe({
            next: (s) => {
                this.referralCount.set(s.totalReferrals);
                this.creditsEarned.set(s.totalCreditsEarned);
                this.rewardPerReferral.set(s.rewardPerReferral);
            },
            error: () => { }
        });
    }

    copyLink(): void {
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

    shareEmail(): void {
        const link = this.referralLink();
        if (link) {
            const subject = encodeURIComponent('Try Gstbuddies — we both get bonus credits!');
            const body = encodeURIComponent(`Hey,\n\nI've been using Gstbuddies for Rule 37 compliance checks and it's great. Sign up with my referral link and we both get bonus credits:\n\n${link}\n\nCheers!`);
            window.open(`mailto:?subject=${subject}&body=${body}`, '_self');
        }
    }
}
