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
  hasResent: boolean = false;
  successMessage: string = '';
  errorMessage: string = '';
  redirectCountdown: number = 3;
  private redirectInterval: any;

  // Expose for template
  isLoading() { return this.verifying; }

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
    if (this.hasResent) return;

    this.resending = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.http.post(`${environment.apiUrl}/auth/api/v1/auth/resend-verification`, { email: this.email })
      .subscribe({
        next: () => {
          this.resending = false;
          this.hasResent = true;
          this.successMessage = 'Verification code has been resent to your email!';
        },
        error: (err) => {
          this.resending = false;
          this.errorMessage = err.error?.message || 'Failed to resend code.';
        }
      });
  }

  ngOnDestroy(): void {
    if (this.redirectInterval) {
      clearInterval(this.redirectInterval);
    }
  }
}
