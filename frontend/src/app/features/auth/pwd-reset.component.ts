import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

// PrimeNG imports
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { PasswordModule } from 'primeng/password';

import { AuthService } from '../../core/auth.service';

type FlowStep = 'email' | 'reset';

@Component({
  selector: 'app-password-reset',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    CardModule,
    InputTextModule,
    ButtonModule,
    MessageModule,
    PasswordModule
  ],
  templateUrl: './pwd-reset.component.html',
  styleUrls: ['./pwd-reset.component.scss']
})
export class PasswordResetComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  // State
  step = signal<FlowStep>('email');
  loading = signal(false);
  error = signal('');
  success = signal('');

  // Form fields
  email = '';
  code = '';
  newPwd = '';
  confirmPwd = '';

  async onRequestCode() {
    if (!this.email) return;

    this.loading.set(true);
    this.error.set('');
    this.success.set('');

    try {
      await this.authService.forgotPassword(this.email);
      this.success.set('Verification code sent to your email.');
      // Move to reset step
      setTimeout(() => {
        this.step.set('reset');
        this.success.set('');
      }, 1500);
    } catch (err: unknown) {
      const error = err as { error?: { message?: string } };
      this.error.set(error?.error?.message || 'Failed to send verification code. Please try again.');
    } finally {
      this.loading.set(false);
    }
  }

  async onResetPassword() {
    if (!this.code || !this.newPwd || !this.confirmPwd) return;

    if (this.newPwd !== this.confirmPwd) {
      this.error.set('Passwords do not match.');
      return;
    }

    this.loading.set(true);
    this.error.set('');
    this.success.set('');

    try {
      await this.authService.resetPassword(this.email, this.code, this.newPwd);
      this.success.set('Password reset successful! Redirecting to login...');
      setTimeout(() => {
        this.router.navigate(['/auth/login']);
      }, 2000);
    } catch (err: unknown) {
      const error = err as { error?: { message?: string } };
      this.error.set(error?.error?.message || 'Failed to reset password. Please check your code and try again.');
    } finally {
      this.loading.set(false);
    }
  }

  async resendCode() {
    this.loading.set(true);
    this.error.set('');
    this.success.set('');

    try {
      await this.authService.forgotPassword(this.email);
      this.success.set('New verification code sent to your email.');
    } catch (err: unknown) {
      const error = err as { error?: { message?: string } };
      this.error.set(error?.error?.message || 'Failed to resend code.');
    } finally {
      this.loading.set(false);
    }
  }

  goBack() {
    this.step.set('email');
    this.code = '';
    this.newPwd = '';
    this.confirmPwd = '';
    this.error.set('');
    this.success.set('');
  }
}
