import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { InputOtpModule } from 'primeng/inputotp';
import { environment } from '../../../../environments/environment';

/**
 * Premium Email Verification Component.
 * Modern glassmorphism design with individual digit inputs and smooth animations.
 */
@Component({
  selector: 'app-verify-email',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, ButtonModule, MessageModule, InputOtpModule],
  templateUrl: './verify-email.component.html',
  styleUrls: ['./verify-email.component.scss']
})
export class VerifyEmailComponent implements OnInit, OnDestroy {
  email: string = '';
  tenantId: string = '';
  otp: string = '';

  verifying: boolean = false;
  resending: boolean = false;
  verified: boolean = false;
  successMessage: string = '';
  errorMessage: string = '';
  cooldownRemaining: number = 0;
  redirectCountdown: number = 3;
  private cooldownInterval: any;
  private redirectInterval: any;

  // Expose for template
  isLoading() { return this.verifying; }
  get resendTimer() { return this.cooldownRemaining; }

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient
  ) { }

  ngOnInit(): void {
    // Get email and tenantId from router state
    this.email = history.state?.email || this.route.snapshot.queryParams['email'];
    this.tenantId = history.state?.tenantId || '';

    console.log('VerifyEmailComponent initialized:', { email: this.email, tenantId: this.tenantId });

    // If no email provided, redirect to signup
    if (!this.email) {
      console.log('No email found, redirecting to signup');
      this.router.navigate(['/auth/signup/personal']);
    }
  }

  onSubmit() {
    this.verifyEmail();
  }

  verifyEmail(): void {
    if (!this.otp || this.otp.length < 6) return;

    this.verifying = true;
    this.errorMessage = '';
    this.successMessage = '';

    const payload = {
      email: this.email,
      code: this.otp,
      tenantId: this.tenantId
    };

    this.http.post(`${environment.apiUrl}/auth/api/v1/auth/signup/verify`, payload)
      .subscribe({
        next: (response: any) => {
          this.verifying = false;
          this.verified = true;
          this.startRedirectCountdown();
        },
        error: (err) => {
          this.verifying = false;
          this.errorMessage = err.error?.message || 'Verification failed. Please try again.';
        }
      });
  }

  private startRedirectCountdown(): void {
    this.redirectCountdown = 3;
    this.redirectInterval = setInterval(() => {
      this.redirectCountdown--;
      if (this.redirectCountdown <= 0) {
        clearInterval(this.redirectInterval);
        this.router.navigate(['/auth/login'], {
          queryParams: { verified: 'true', email: this.email }
        });
      }
    }, 1000);
  }

  resendCode(): void {
    if (this.cooldownRemaining > 0) return;

    this.resending = true;
    this.errorMessage = '';

    this.http.post(`${environment.apiUrl}/auth/api/v1/auth/resend-verification`, { email: this.email })
      .subscribe({
        next: () => {
          this.resending = false;
          this.successMessage = 'Verification code sent!';
          this.startCooldown(60);
          // Clear success message after 3 seconds
          setTimeout(() => this.successMessage = '', 3000);
        },
        error: (err) => {
          this.resending = false;
          this.errorMessage = err.error?.message || 'Failed to resend code.';
        }
      });
  }

  private startCooldown(seconds: number): void {
    this.cooldownRemaining = seconds;

    if (this.cooldownInterval) {
      clearInterval(this.cooldownInterval);
    }

    this.cooldownInterval = setInterval(() => {
      this.cooldownRemaining--;
      if (this.cooldownRemaining <= 0) {
        clearInterval(this.cooldownInterval);
      }
    }, 1000);
  }

  ngOnDestroy(): void {
    if (this.cooldownInterval) {
      clearInterval(this.cooldownInterval);
    }
    if (this.redirectInterval) {
      clearInterval(this.redirectInterval);
    }
  }
}
